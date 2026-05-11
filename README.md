# EarthquakeApp

## Local setup

Google Maps API key should not be committed to the repository.

Use one of these options:

```properties
# local.properties
MAPS_API_KEY=your_maps_api_key
```

or

```bash
export MAPS_API_KEY=your_maps_api_key
```

`app/build.gradle` reads `MAPS_API_KEY` from `local.properties` first, then falls back to the environment variable.
