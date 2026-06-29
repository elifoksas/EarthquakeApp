import fs from "node:fs/promises";
import path from "node:path";

const ROOT = path.resolve(import.meta.dirname, "..");
const OUTPUT = path.join(ROOT, "app/src/main/res/raw/emergency_numbers.json");
const ITU_URL =
  "https://www.itu.int/net/itu-t/inrdb/e129_important_numbers.aspx";
const SHORT_METADATA_URL =
  "https://raw.githubusercontent.com/google/libphonenumber/master/resources/ShortNumberMetadata.xml";
const PHONE_METADATA_URL =
  "https://raw.githubusercontent.com/google/libphonenumber/master/resources/PhoneNumberMetadata.xml";
const SNAPSHOT_DATE = new Date().toISOString().slice(0, 10);
const ISO_REGIONS_WITHOUT_PHONE_METADATA = [
  "AQ",
  "BV",
  "GS",
  "HM",
  "PN",
  "TF",
  "UM",
];

const COUNTRY_ALIASES = {
  "bolivia plurinational state of": "BO",
  "bosnia and herzegovina": "BA",
  "brunei darussalam": "BN",
  "cabo verde": "CV",
  "central african rep": "CF",
  "congo": "CG",
  "cote divoire": "CI",
  "czech rep": "CZ",
  "dem rep of the congo": "CD",
  "iran islamic republic of": "IR",
  "korea rep of": "KR",
  kosovo: "XK",
  "moldova republic of": "MD",
  "netherlands kingdom of the": "NL",
  "north macedonia": "MK",
  "russian federation": "RU",
  "syrian arab republic": "SY",
  "tanzania": "TZ",
  "turkiye": "TR",
  "united states": "US",
  "venezuela bolivarian republic of": "VE",
};

const SERVICE_CATEGORIES = {
  emergency: "GENERAL",
  police: "POLICE",
  fire: "FIRE",
  medical: "AMBULANCE",
};

const VERIFIED_OVERRIDES = {
  TR: {
    services: [
      {
        number: "112",
        categories: ["GENERAL", "AMBULANCE", "FIRE", "POLICE"],
      },
    ],
    sourceUrl: "https://www.112.gov.tr/",
    verifiedAt: SNAPSHOT_DATE,
  },
};

function normalize(value) {
  return value
    .normalize("NFKD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/&/g, "and")
    .replace(/[^a-zA-Z0-9]+/g, " ")
    .trim()
    .toLowerCase();
}

function decodeHtml(value) {
  return value
    .replace(/<[^>]+>/g, "")
    .replace(/&amp;/g, "&")
    .replace(/&#39;/g, "'")
    .replace(/&quot;/g, '"')
    .replace(/&nbsp;/g, " ")
    .trim();
}

function spanValue(html, prefix, index) {
  const expression = new RegExp(
    `id="${prefix}_${index}">([\\s\\S]*?)<\\/span>`,
  );
  return decodeHtml(expression.exec(html)?.[1] ?? "");
}

function parseTerritoryIds(xml) {
  return [
    ...new Set(
      [...xml.matchAll(/<territory id="([A-Z]{2})"/g)].map(
        (match) => match[1],
      ),
    ),
  ].sort();
}

function parseEmergencyExamples(xml) {
  const result = new Map();
  for (const match of xml.matchAll(
    /<territory id="([A-Z]{2})">([\s\S]*?)<\/territory>/g,
  )) {
    const emergency = /<emergency>([\s\S]*?)<\/emergency>/.exec(match[2])?.[1];
    const example = emergency
      ? /<exampleNumber>([^<]+)<\/exampleNumber>/.exec(emergency)?.[1]?.trim()
      : null;
    if (example) result.set(match[1], example);
  }
  return result;
}

function parseCountryOptions(html) {
  return [...html.matchAll(/<option value="(\d+)">([^<]+)<\/option>/g)]
    .map((match) => ({ id: match[1], name: decodeHtml(match[2]) }))
    .filter((country) => country.id !== "0");
}

function parseItuRows(html) {
  const rows = [];
  for (const match of html.matchAll(/lblCountry_(\d+)">/g)) {
    const index = match[1];
    const category = spanValue(
      html,
      "ContentPlaceHolder1_GridView1_lblcategory_service_name",
      index,
    );
    const mappedCategory = SERVICE_CATEGORIES[normalize(category)];
    if (!mappedCategory) continue;

    const numbers = spanValue(
      html,
      "ContentPlaceHolder1_GridView1_lblNumber",
      index,
    ).match(/\d{2,6}/g);
    if (!numbers?.length) continue;

    for (const number of numbers) {
      rows.push({
        country: spanValue(
          html,
          "ContentPlaceHolder1_GridView1_lblCountry",
          index,
        ),
        number,
        category: mappedCategory,
        updatedAt: spanValue(
          html,
          "ContentPlaceHolder1_GridView1_lbllast_update",
          index,
        ),
      });
    }
  }
  return rows;
}

async function fetchText(url) {
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`Request failed (${response.status}): ${url}`);
  }
  return response.text();
}

async function mapConcurrent(values, concurrency, mapper) {
  const output = new Array(values.length);
  let cursor = 0;
  async function worker() {
    while (cursor < values.length) {
      const index = cursor++;
      output[index] = await mapper(values[index], index);
    }
  }
  await Promise.all(
    Array.from({ length: Math.min(concurrency, values.length) }, worker),
  );
  return output;
}

const [indexHtml, shortMetadata, phoneMetadata] = await Promise.all([
  fetchText(`${ITU_URL}?pg_size=50`),
  fetchText(SHORT_METADATA_URL),
  fetchText(PHONE_METADATA_URL),
]);

const territoryIds = [
  ...new Set([
    ...parseTerritoryIds(phoneMetadata),
    ...ISO_REGIONS_WITHOUT_PHONE_METADATA,
  ]),
].sort();
const emergencyExamples = parseEmergencyExamples(shortMetadata);
const displayNames = new Intl.DisplayNames(["en"], { type: "region" });
const isoByName = new Map(
  territoryIds.map((isoCode) => [
    normalize(displayNames.of(isoCode) ?? isoCode),
    isoCode,
  ]),
);
Object.entries(COUNTRY_ALIASES).forEach(([name, isoCode]) => {
  isoByName.set(normalize(name), isoCode);
});

const ituCountries = parseCountryOptions(indexHtml);
const countryPages = await mapConcurrent(ituCountries, 8, async (country) => {
  const html = await fetchText(`${ITU_URL}?country=${country.id}&pg_size=100`);
  return parseItuRows(html);
});
const ituRows = countryPages.flat();
const ituByIso = new Map();

for (const row of ituRows) {
  const isoCode = isoByName.get(normalize(row.country));
  if (!isoCode) continue;
  const rows = ituByIso.get(isoCode) ?? [];
  rows.push(row);
  ituByIso.set(isoCode, rows);
}

const profiles = territoryIds.map((isoCode) => {
  const override = VERIFIED_OVERRIDES[isoCode];
  if (override) {
    return {
      isoCode,
      countryName: displayNames.of(isoCode) ?? isoCode,
      ...override,
    };
  }

  const ituServices = ituByIso.get(isoCode) ?? [];
  const grouped = new Map();
  for (const service of ituServices) {
    const categories = grouped.get(service.number) ?? new Set();
    categories.add(service.category);
    grouped.set(service.number, categories);
  }

  if (grouped.size === 0 && emergencyExamples.has(isoCode)) {
    grouped.set(emergencyExamples.get(isoCode), new Set(["GENERAL"]));
  }

  const latestItuDate = ituServices
    .map((service) => service.updatedAt)
    .filter(Boolean)
    .sort()
    .at(-1);
  const hasItuData = ituServices.length > 0;

  return {
    isoCode,
    countryName: displayNames.of(isoCode) ?? isoCode,
    services: [...grouped.entries()]
      .map(([number, categories]) => ({
        number,
        categories: [...categories].sort(),
      }))
      .sort((left, right) => {
        const leftGeneral = left.categories.includes("GENERAL") ? 0 : 1;
        const rightGeneral = right.categories.includes("GENERAL") ? 0 : 1;
        return leftGeneral - rightGeneral || left.number.localeCompare(right.number);
      }),
    sourceUrl: hasItuData ? ITU_URL : SHORT_METADATA_URL,
    verifiedAt: latestItuDate || SNAPSHOT_DATE,
  };
});

await fs.mkdir(path.dirname(OUTPUT), { recursive: true });
await fs.writeFile(
  OUTPUT,
  `${JSON.stringify(
    {
      generatedAt: SNAPSHOT_DATE,
      profiles,
    },
    null,
    2,
  )}\n`,
);

const profilesWithNumbers = profiles.filter(
  (profile) => profile.services.length > 0,
).length;
console.log(
  `Generated ${profiles.length} profiles (${profilesWithNumbers} with verified numbers) at ${OUTPUT}`,
);
