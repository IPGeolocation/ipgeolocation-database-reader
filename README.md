# ipgeolocation.io Database Reader

This is a step-by-step guide on how to deploy the ipgeolocation-database-reader-2.0.1.war Java API and consume API responses.

## What's New

### 2.0.1
- **MMDB zero-downtime updates**: MMDB readers are now hot-swapped on update without restarting Tomcat, eliminating the 502 error window that occurred between shutdown and restart.
- **Fixed MMDB crash on update**: new database files are written to a temporary path first and moved atomically to the live path, preventing the `InternalError` that occurred when a request hit the reader mid-overwrite.
- **Faster database extraction**: ZIP extraction buffer increased from 1 KB to 64 KB, reducing extraction time from up to 30 minutes to seconds.

### 2.0.0
- For `DB-V`, `DB-VI` and `DB-VII`, the security database has been updated from a single IP format to an IP range format, optimizing memory usage while providing broader IP coverage.

## Requirements

- JDK 8 (This reader is built and tested using JDK 1.8).
- For CSV databases, at least 4-8 GB RAM (for smaller CSV databases) and maximum 16 GB RAM for larger databases like DB-IV, DB-VI, and DB-VII.
- For MMDB databases, at least 2-4 GB RAM only.
- ipgeolocation-database-reader-2.0.1.war provided with this archive.

## Basic Usage

### For Linux

Follow the steps below (commands against each step are also provided) to deploy and consume the **IP to City+ISP+Proxy database**:

- Create ~/conf/ipgeolocation directory in the 'home' directory.
  ```bash
  mkdir -p ~/conf/ipgeolocation
  ```
- Create 'database-reader-config.yaml' file in the ~/conf/ipgeolocation directory.
  ```bash
  vi ~/conf/ipgeolocation/database-reader-config.yaml
  ```
* Write the following YAML config values in it (You can use any editor of your choice like `nano`. I am using `vim`.):
  ```yaml
  ipgeolocation:
    database:
      workingDirectory: "/etc/ipgeolocation"
      apiKey: "YOUR_API_KEY"
      version: "DB-VII"
      updateInterval: "week"
      type: "mmdb"
      autoFetchAndUpdate: false
  ```
    * Against `workingDirectory` key, replace `/etc/ipgeolocation` value with the directory path where you want to place the
      database files.
    * Against `apiKey` key, replace `YOUR_API_KEY` value with the API key from your database subscription.
    * Against `version` key, replace `DB-VII` value with the database version that you've subscribed to. It can be `DB-I`, `DB-II`, `DB-III`, `DB-IV`, `DB-V`, `DB-VI`, or `DB-VII`.
    * Against `updateInterval` key, replace `week` value with your database subscription update interval. It can be `week`, or `month`.
    * Against `type` key, replace `mmdb` value with your choice of database to query from. It can be `csv`, or `mmdb`. It's default value is `mmdb` and can be skipped from configuration.
    * Against `autoFetchAndUpdate` key, the value can be `true` or `false`.
      - If set to `true`, the database reader will automatically download the latest database as soon as it is available. For MMDB, the new database is loaded in-memory without any restart or downtime. For CSV, the service restarts to reload the updated database.
      - If set to `false`, the database reader will not check for the updated database for you. You can send a POST request to `/database/update` endpoint to fetch and update the database in-memory if an update is available. For example, here is a cURL request: `curl --location --request POST 'http://address-to-api:8080/database/update'`
      - It's default value is `true` and can be skipped from configuration.
- Run the WAR file (if you placed config file at default path `~/conf/ipgeolocation`)
    ```bash
    java -jar -Xms6G -Xmx10G /path/to/ipgeolocation-database-reader-2.0.1.war
    ```  
    Note: Use -Xms<ram_size1> flag to set the minimum RAM and -Xmx<ram_size2> to set the maximum RAM to be allocated to execute the 'ipgeolocation-database-reader-2.0.1.war' application.
    Note: ram_size1 is the minimum RAM, and ram_size2 is the maximum RAM allocated to the application.
    Note: You can deploy the WAR file in an embedded container like Apache Tomcat as well.

The database reader will download the latest database and load it in-memory while bootstrapping and will update the database as soon as the new update is available, if `autoFetchAndUpdate` is set to `true`.  

**Note:** For MMDB, the reader hot-swaps the database in-memory with no restart and no downtime. For CSV, the reader restarts after fetching the latest database because reloading large CSV databases in-memory without a restart would require up to double the RAM.

### Other Ways to Provide Configurations

By default, the database reader will look at `~/conf/ipgeolocation/database-reader-config.yaml` path for the YAML file. You can provide the YAML configurations from your custom location as well. Let's assume that you've created the YAML configurations at `/etc/ipgeolocation/ipgeo-db-reader-conf.yaml`. Here is how you can pass it to the database reader:
```bash
java -Dspring.config.import=file:/etc/ipgeolocation/ipgeo-db-reader-conf.yaml -jar -Xms6G -Xmx10G /path/to/ipgeolocation-database-reader-2.0.1.war
```

### For Windows

Follow the steps below (commands against each step are also provided) to deploy and consume the **IP to City+ISP+Proxy database**:

- Create C:\conf\ipgeolocation folders in the 'C' or any other drive of your choice.
```bash
C:
mkdir conf\ipgeolocation
```
- Create 'database-reader-config.yaml' file in the C:\conf\ipgeolocation directory.
  ```bash
  cd conf\ipgeolocation
  copy NULL database-reader-config.yml
  ```
* Write the following YAML config values in it (You can use any editor of your choice.):
  ```yaml
  ipgeolocation:
    database:
      workingDirectory: "C:\\conf\\ipgeolocation"
      apiKey: "YOUR_API_KEY"
      version: "DB-VII"
      updateInterval: "week"
      type: "mmdb"
      autoFetchAndUpdate: false
  ```
    * Against `workingDirectory` key, replace `C:\\conf\\ipgeolocation` value with the directory path where you want to place the
      database files.
    * Against `apiKey` key, replace `YOUR_API_KEY` value with the API key from your database subscription.
    * Against `version` key, replace `DB-VII` value with the database version that you've subscribed to. It can be `DB-I`, `DB-II`, `DB-III`, `DB-IV`, `DB-V`, `DB-VI`, or `DB-VII`.
    * Against `updateInterval` key, replace `week` value with your database subscription update interval. It can be `week`, or `month`.
    * Against `type` key, replace `mmdb` value with your choice of database to query from. It can be `csv`, or `mmdb`. It's default value is `mmdb` and can be skipped from configuration.
    * Against `autoFetchAndUpdate` key, the value can be `true` or `false`.
      - If set to `true`, the database reader will automatically download the latest database as soon as it is available. For MMDB, the new database is loaded in-memory without any restart or downtime. For CSV, the service restarts to reload the updated database.
      - If set to `false`, the database reader will not check for the updated database for you. You can send a POST request to `/database/update` endpoint to fetch and update the database in-memory if an update is available. For example, here is a cURL request: `curl --location --request POST 'http://address-to-api:8080/database/update'`
      - It's default value is `true` and can be skipped from configuration.
- Run the WAR file
    ```bash
    java -jar -Xms4G -Xmx4G -Dspring.config.import=file:C:\\conf\\ipgeolocation\\database-reader-config.yml C:\path\to\ipgeolocation-database-reader-2.0.1.war
    ```  
    Note: Use -Xms<ram_size1> flag to set the minimum RAM and -Xmx<ram_size2> to set the maximum RAM to be allocated to execute the 'ipgeolocation-database-reader-2.0.1.war' application.
    Note: ram_size1 is the minimum RAM, and ram_size2 is the maximum RAM allocated to the application.
    Note: You can deploy the WAR file in an embedded container like Apache Tomcat as well.

The database reader will download the latest database and load it in-memory while bootstrapping and will update the database as soon as the new update is available, if `autoFetchAndUpdate` is set to `true`.  

**Note:** For MMDB, the reader hot-swaps the database in-memory with no restart and no downtime. For CSV, the reader restarts after fetching the latest database because reloading large CSV databases in-memory without a restart would require up to double the RAM.

Another way is not create a YAML file and provide the configurations as the command-line arguments to the `war` file. Here is how you can do that:

```bash
java -Dipgeolocation.database.workingDirectory=/etc/ipgeolocation -Dipgeolocation.database.apiKey=YOUR_API_KEY -Dipgeolocation.database.version=DB-VII -Dipgeolocation.database.updateInterval=week -Dipgeolocation.database.type=mmdb -Dipgeolocation.database.autoFetchAndUpdate=false -jar -Xms6G -Xmx10G /path/to/ipgeolocation-database-reader-2.0.1.war
```

## How to Get IP Geolocation

ipgeolocation.io Database Reader runs at 8080 port by default. The reader will take about 10 seconds to 6 minutes to deploy the updated database files based on the chosen configurations.

**Note:** All the examples assume that the reader has been deployed on the `localhost` at port 8080.

### Single IP Geolocation Lookup

For single IP Geolocation lookup for any IPv4 or IPv6 address with JSON response, the URL for this endpoint is `http://localhost:8080/ipGeo` and its full JSON response is below:

```json
{
    "ip": "8.8.8.8",
    "continent_code": "NA",
    "continent_name": "North America",
    "country_code2": "US",
    "country_code3": "USA",
    "country_name": "United States",
    "country_capital": "Washington",
    "state_prov": "California",
    "district": "",
    "city": "Mountain View",
    "zipcode": "94043",
    "latitude": "37.4229",
    "longitude": "-122.085",
    "is_eu": false,
    "calling_code": "+1",
    "country_tld": ".us",
    "languages": "en-US,es-US,haw,fr",
    "country_flag": "https://ipGeolocation.io/static/flags/us_64.png",
    "isp": "Level 3 Communications",
    "connection_type": "",
    "organization": "Google Inc.",
    "geoname_id": "5375480",
    "currency": {
        "code": "USD",
        "name": "US Dollar",
        "symbol": "$"
    },
    "time_zone": {
        "name": "America/Los_Angeles",
        "offset": -8,
        "current_time": "2019-01-14 03:30:00.135-0800",
        "current_time_unix": 1547465400.135,
        "is_dst": false,
        "dst_savings": 1
    },
    "security": {
        "threat_score": 0,
        "is_tor": false,
        "is_proxy": false,
        "proxy_type": "",
        "is_anonymous": false,
        "is_known_attacker": false,
        "is_cloud_provider": false
    }
}
```

### Passing an IPv4, or IPv6 Address

In order to find geolocation information for any IP address, pass it as a query parameter like below. This endpoint is meant to be called from the server side.

#### Get geolocation for an IPv4 IP Address = 1.1.1.1

```bash
curl 'http://localhost:8080/ipGeo?ip=1.1.1.1'
```

#### Get geolocation for an IPv6 IP Address = 2001:4860:4860::1

```bash
curl 'http://localhost:8080/ipGeo?ip=2001:4860:4860::1'
```

#### Without Passing an IP Address

When the IP address is not present, it returns the geolocation information of the device/client which is calling it. This endpoint is meant to be called from client side.

```bash
curl 'http://localhost:8080/ipGeo'
```  

** IP address is always included in the API response

### Bulk IP Geolocation Lookup

This endpoint allows you to perform the lookup of multiple IPv4 and IPv6 addresses (max. 50) at the same time.  
To perform bulk IP Geolocation Lookup, send a POST request and pass the `ips` array as JSON data along with it. Here is an example:

```bash
curl -X POST 'http://localhost:8080/ipGeoBulk' -H 'Content-Type: application/json' -d '{ "ips": ["1.1.1.1", "1.2.3.4"] }'
```

### Response in Multiple Languages

The geolocation information for an IP address can be retrieved in the following languages:
- English (en)
- German (de)
- Russian (ru)
- Japanese (ja)
- French (fr)
- Chinese Simplified (cn)
- Spanish (es)
- Czech (cs)
- Italian (it)

By default, the reader responds in English. You can change the response language by passing the language code as a query parameter `lang`. Here are a few cURL examples:

#### Get geolocation for an IPv4 IP Address = 1.1.1.1 in Chinese language

```bash
curl 'http://localhost:8080/ipGeo?ip=1.1.1.1&lang=cn'
```

#### Get details for an IPv6 IP Address = 2001:4860:4860::1 in Russian

```bash
curl 'http://localhost:8080/ipGeo?ip=2001:4860:4860::1&lang=ru'
```

### Filter Responses

We've built the reader to give you fine granularity. Specify what you want in query parameter and get only those fields. This will save processing time, bandwidth and improve the API response time.  

You can filter the API response in two ways:

#### Get the Required Fields Only

First, you can filter the reader response by specifying names of the fields that you want instead of getting the full response. Names of the required fields must be passed as a query parameter fields in the request. Here are a few examples to get only the required fields:

##### Get City Information Only

```bash
curl 'http://localhost:8080/ipGeo?ip=1.1.1.1&fields=city'
```
```json
{
    "ip": "1.1.1.1",
    "city": "South Brisbane"
}
```

##### Get Country Name and Country Code (ISO2) Only

```bash
curl 'http://localhost:8080/ipGeo?ip=1.1.1.1&fields=country_code2,country_name'
```
```json
{
    "ip": "1.1.1.1",
    "country_code2": "AU",
    "country_name": "Australia"
}
```

##### Get the Time Zone Information Only

```bash
curl 'http://localhost:8080/ipGeo?ip=1.1.1.1&fields=time_zone'
```
```json
{
    "ip": "1.1.1.1",
    "time_zone": {
        "name": "America/Los_Angeles",
        "offset": -8,
        "current_time": "2018-12-06 00:28:40.339-0800",
        "current_time_unix": 1544084920.339,
        "is_dst": false,
        "dst_savings": 1
    }
}
```

##### Get Only the Local Currency Information of Multiple IP Addresses

You can use the filters with Bulk IP Lookup as well. Here is an example:
```bash
curl -X POST 'http://localhost:8080/ipGeoBulk?fields=currency' -H 'Content-Type: application/json' -d '{ "ips": ["1.1.1.1", "1.2.3.4"] }'
```
```json
[
    {
        "ip": "1.1.1.1",
        "currency": {
            "name": "Australian Dollar",
            "code": "AUD",
            "symbol": "A$"
        }
    },
    {
        "ip": "1.2.3.4",
        "currency": {
            "name": "Australian Dollar",
            "code": "AUD",
            "symbol": "A$"
        }
    }
]
```

##### Get Geo Location Only

We know, usually, the users are interested in geolocation information only. So, we have added a shortcut for you. You can specify just `fields=geo` in the query parameter.

```bash
curl 'http://localhost:8080/ipGeo?ip=1.1.1.1&fields=geo'
```
```json
{
    "ip": "1.1.1.1",
    "country_code2": "AU",
    "country_code3": "AUS",
    "country_name": "Australia",
    "state_prov": "Queensland",
    "district": "Brisbane",
    "city": "South Brisbane",
    "zipcode": "4101",
    "latitude": "-27.4748",
    "longitude": "153.017"
}
```

#### Remove the Unnecessary Fields

Second, you can also filter the reader response by specifying the names of fields (except IP address) that you want to remove from the API response. Names of the fields must be passed as a query parameter `excludes` in the request. Here are a few examples to exclude the unnecessary fields from the API response:

##### Exclude Continent Code, Currency and, Time zone Objects

```bash
curl 'http://localhost:8080/ipGeo?ip=1.1.1.1&excludes=continent_code,currency,time_zone'
```
```json
{
    "ip": "1.1.1.1",
    "continent_name": "Oceania",
    "country_code2": "AU",
    "country_code3": "AUS",
    "country_name": "Australia",
    "country_capital": "Canberra",
    "state_prov": "Queensland",
    "district": "Brisbane",
    "city": "South Brisbane",
    "zipcode": "4101",
    "latitude": "-27.4748",
    "longitude": "153.017",
    "is_eu": false,
    "calling_code": "+61",
    "country_tld": ".au",
    "languages": "en-AU",
    "country_flag": "https://ipGeolocation.io/static/flags/au_64.png",
    "isp": "Cloudflare Inc.",
    "connection_type": "",
    "organization": "",
    "geoname_id": "2207259"
}
```

##### Get the Geo Field and Exclude Continent Information

```bash
curl 'http://localhost:8080/ipGeo?ip=1.1.1.1&fields=geo&excludes=continent_code,continent_name'
```
```json
{
    "ip": "1.1.1.1",
    "country_code2": "AU",
    "country_code3": "AUS",
    "country_name": "Australia",
    "state_prov": "Queensland",
    "district": "Brisbane",
    "city": "South Brisbane",
    "zipcode": "4101",
    "latitude": "-27.4748",
    "longitude": "153.017"
}
```

## IP-Security Information for an IP Address

The reader also provides IP-Security information, but doesn't respond it by default. To get IP-Security information along with geolocation information, you must pass the `include=security` as a query parameter in the URL.

Here is an example to IP Geolocation lookup that includes IP security information for the IP address:

```bash
curl 'http://localhost:8080/ipGeo?ip=198.90.78.238&fields=geo&include=security'
```
```json
{
    "ip": "198.90.78.238",
    "country_code2": "CA",
    "country_code3": "CAN",
    "country_name": "Canada",
    "state_prov": "Quebec",
    "district": "Sainte-Rose",
    "city": "Laval",
    "zipcode": "H7P 4W5",
    "latitude": "45.58160",
    "longitude": "-73.76980",
    "security": {
        "threat_score": 7,
        "is_tor": false,
        "is_proxy": true,
        "proxy_type": "VPN",
        "is_anonymous": true,
        "is_known_attacker": false,
        "is_cloud_provider": false
    }
}
```

## Error Codes

The reader returns 200 HTTP status in case of a successful request.  

While, in case of an illegal request, reader returns 4xx HTTP code alongs with a descriptive message as why the error occurred.

Here is the description of why a specific HTTP code is returned:

| HTTP Status | Description                                                                              |
|-------------|------------------------------------------------------------------------------------------|
| 400         | If the queried IP address or domain name is not valid.                                   |
| 404         | If the queried IP address or domain name is not found in our database.                   |
| 423         | If the queried IP address is a bogon (reserved) IP address like private, multicast, etc. |


## Sample Responses
Below are some sample responses for all supported databases, demonstrating different levels of IP geolocation, ISP, and security insights.
#### DB-I (IP TO COUNTRY)
#### Overview
The IP-to-Country response provides geographic and country-level details associated with a given IP address. This data is useful for analytics, localization, compliance, and security purposes.

```json
{
    "ip": "1.1.1.1",
    "continent_code": "NA",
    "continent_name": "North America",
    "country_code2": "US",
    "country_code3": "USA",
    "country_name": "United States",
    "country_capital": "Washington, D.C.",
    "is_eu": false,
    "calling_code": "+1",
    "country_tld": ".us",
    "languages": "en-US,es-US,haw,fr",
    "country_flag": "https://ipgeolocation.io/static/flags/us_64.png",
    "currency": {
        "code": "USD",
        "name": "US Dollar",
        "symbol": "$"
    }
}
```
#### For Bulk lookup

Sample Response for Two IPs:
<details>
<summary>Click to expand response</summary>

```json
[
  {
    "ip": "1.0.0.0",
    "continent_code": "OC",
    "continent_name": "Oceania",
    "country_code2": "AU",
    "country_code3": "AUS",
    "country_name": "Australia",
    "country_capital": "Canberra",
    "is_eu": false,
    "calling_code": "+61",
    "country_tld": ".au",
    "languages": "en-AU",
    "country_flag": "https://ipgeolocation.io/static/flags/au_64.png",
    "currency": {
      "code": "AUD",
      "name": "Australian Dollar",
      "symbol": "A$"
    }
  },
  {
    "ip": "1.0.0.1",
    "continent_code": "NA",
    "continent_name": "North America",
    "country_code2": "US",
    "country_code3": "USA",
    "country_name": "United States",
    "country_capital": "Washington, D.C.",
    "is_eu": false,
    "calling_code": "+1",
    "country_tld": ".us",
    "languages": "en-US,es-US,haw,fr",
    "country_flag": "https://ipgeolocation.io/static/flags/us_64.png",
    "currency": {
      "code": "USD",
      "name": "US Dollar",
      "symbol": "$"
    }
  }
]
```
</details>


### DB-II (IP TO CITY)

#### Overview
Offers more granular location data, such as the state/province, city name, ZIP code, latitude, longitude, and time zone.

```json
{
    "ip": "1.1.1.1",
    "continent_code": "NA",
    "continent_name": "North America",
    "country_code2": "US",
    "country_code3": "USA",
    "country_name": "United States",
    "country_capital": "Washington, D.C.",
    "state_prov": "California",
    "district": "",
    "city": "Los Angeles",
    "zipcode": "90012",
    "latitude": "34.05361",
    "longitude": "-118.24550",
    "geoname_id": "5332870",
    "is_eu": false,
    "calling_code": "+1",
    "country_tld": ".us",
    "languages": "en-US,es-US,haw,fr",
    "country_flag": "https://ipgeolocation.io/static/flags/us_64.png",
    "currency": {
        "code": "USD",
        "name": "US Dollar",
        "symbol": "$"
    },
    "time_zone": {
        "name": "America/Los_Angeles",
        "offset": -8,
        "current_time": "2025-02-10 01:42:42.018-0800",
        "current_time_unix": 1739180562,
        "is_dst": false,
        "dst_savings": 0
    }
}
```

#### For Bulk lookup

Sample Response for Two IPs:
<details>
<summary>Click to expand response</summary>

```json
[
  {
    "ip": "1.0.0.0",
    "continent_code": "OC",
    "continent_name": "Oceania",
    "country_code2": "AU",
    "country_code3": "AUS",
    "country_name": "Australia",
    "country_capital": "Canberra",
    "state_prov": "Queensland",
    "district": "",
    "city": "Brisbane",
    "zipcode": "4101",
    "latitude": "-27.47306",
    "longitude": "153.01421",
    "geoname_id": "10113228",
    "is_eu": false,
    "calling_code": "+61",
    "country_tld": ".au",
    "languages": "en-AU",
    "country_flag": "https://ipgeolocation.io/static/flags/au_64.png",
    "currency": {
      "code": "AUD",
      "name": "Australian Dollar",
      "symbol": "A$"
    },
    "time_zone": {
      "name": "Australia/Brisbane",
      "offset": 10,
      "current_time": "2025-02-10 22:08:33.296+1000",
      "current_time_unix": 1739189313,
      "is_dst": false,
      "dst_savings": 0
    }
  },
  {
    "ip": "1.0.0.1",
    "continent_code": "NA",
    "continent_name": "North America",
    "country_code2": "US",
    "country_code3": "USA",
    "country_name": "United States",
    "country_capital": "Washington, D.C.",
    "state_prov": "California",
    "district": "",
    "city": "Los Angeles",
    "zipcode": "90012",
    "latitude": "34.05361",
    "longitude": "-118.24550",
    "geoname_id": "5332870",
    "is_eu": false,
    "calling_code": "+1",
    "country_tld": ".us",
    "languages": "en-US,es-US,haw,fr",
    "country_flag": "https://ipgeolocation.io/static/flags/us_64.png",
    "currency": {
      "code": "USD",
      "name": "US Dollar",
      "symbol": "$"
    },
    "time_zone": {
      "name": "America/Los_Angeles",
      "offset": -8,
      "current_time": "2025-02-10 04:08:33.300-0800",
      "current_time_unix": 1739189313,
      "is_dst": false,
      "dst_savings": 0
    }
  }
]
```
</details>

### DB-III (IP TO ISP)

#### Overview
Provides country-level information, including the continent, country name, and currency of an IP, along with details about the ISP, ASN, and organization.
```json
{
  "ip": "1.1.1.1",
  "continent_code": "NA",
  "continent_name": "North America",
  "country_code2": "US",
  "country_code3": "USA",
  "country_name": "United States",
  "country_capital": "Washington, D.C.",
  "is_eu": false,
  "calling_code": "+1",
  "country_tld": ".us",
  "languages": "en-US,es-US,haw,fr",
  "country_flag": "https://ipgeolocation.io/static/flags/us_64.png",
  "isp": "APNIC Research and Development",
  "connection_type": "",
  "organization": "Cloudflare, Inc.",
  "asn": "AS13335",
  "currency": {
    "code": "USD",
    "name": "US Dollar",
    "symbol": "$"
  }
}
```
#### For Bulk lookup

Sample Response for Two IPs:
<details>
<summary>Click to expand response</summary>

```json
[
  {
    "ip": "1.0.0.0",
    "continent_code": "OC",
    "continent_name": "Oceania",
    "country_code2": "AU",
    "country_code3": "AUS",
    "country_name": "Australia",
    "country_capital": "Canberra",
    "is_eu": false,
    "calling_code": "+61",
    "country_tld": ".au",
    "languages": "en-AU",
    "country_flag": "https://ipgeolocation.io/static/flags/au_64.png",
    "isp": "APNIC Research and Development",
    "connection_type": "",
    "organization": "Cloudflare, Inc.",
    "asn": "AS13335",
    "currency": {
      "code": "AUD",
      "name": "Australian Dollar",
      "symbol": "A$"
    }
  },
  {
    "ip": "1.0.0.1",
    "continent_code": "NA",
    "continent_name": "North America",
    "country_code2": "US",
    "country_code3": "USA",
    "country_name": "United States",
    "country_capital": "Washington, D.C.",
    "is_eu": false,
    "calling_code": "+1",
    "country_tld": ".us",
    "languages": "en-US,es-US,haw,fr",
    "country_flag": "https://ipgeolocation.io/static/flags/us_64.png",
    "isp": "APNIC Research and Development",
    "connection_type": "",
    "organization": "Cloudflare, Inc.",
    "asn": "AS13335",
    "currency": {
      "code": "USD",
      "name": "US Dollar",
      "symbol": "$"
    }
  }
]
```
</details>

### DB-IV (IP TO CITY+ISP)

#### Overview
Combines city-level geolocation with ISP details, including ASN, organization, and connection type, enhancing insights into network infrastructure and service provider data.
```json
{
  "ip": "1.1.1.1",
  "continent_code": "NA",
  "continent_name": "North America",
  "country_code2": "US",
  "country_code3": "USA",
  "country_name": "United States",
  "country_capital": "Washington, D.C.",
  "state_prov": "California",
  "district": "",
  "city": "Los Angeles",
  "zipcode": "90012",
  "latitude": "34.05361",
  "longitude": "-118.24550",
  "geoname_id": "5332870",
  "is_eu": false,
  "calling_code": "+1",
  "country_tld": ".us",
  "languages": "en-US,es-US,haw,fr",
  "country_flag": "https://ipgeolocation.io/static/flags/us_64.png",
  "isp": "APNIC Research and Development",
  "connection_type": "",
  "organization": "Cloudflare, Inc.",
  "asn": "AS13335",
  "currency": {
    "code": "USD",
    "name": "US Dollar",
    "symbol": "$"
  },
  "time_zone": {
    "name": "America/Los_Angeles",
    "offset": -8,
    "current_time": "2025-02-10 01:51:33.942-0800",
    "current_time_unix": 1739181093,
    "is_dst": false,
    "dst_savings": 0
  }
}
```

#### For Bulk lookup

Sample Response for Two IPs:
<details>
<summary>Click to expand response</summary>

```json
[
  {
    "ip": "1.0.0.0",
    "continent_code": "OC",
    "continent_name": "Oceania",
    "country_code2": "AU",
    "country_code3": "AUS",
    "country_name": "Australia",
    "country_capital": "Canberra",
    "state_prov": "Queensland",
    "district": "",
    "city": "Brisbane",
    "zipcode": "4101",
    "latitude": "-27.47306",
    "longitude": "153.01421",
    "geoname_id": "10113228",
    "is_eu": false,
    "calling_code": "+61",
    "country_tld": ".au",
    "languages": "en-AU",
    "country_flag": "https://ipgeolocation.io/static/flags/au_64.png",
    "isp": "APNIC Research and Development",
    "connection_type": "",
    "organization": "Cloudflare, Inc.",
    "asn": "AS13335",
    "currency": {
      "code": "AUD",
      "name": "Australian Dollar",
      "symbol": "A$"
    },
    "time_zone": {
      "name": "Australia/Brisbane",
      "offset": 10,
      "current_time": "2025-02-10 22:04:17.215+1000",
      "current_time_unix": 1739189057,
      "is_dst": false,
      "dst_savings": 0
    }
  },
  {
    "ip": "1.0.0.1",
    "continent_code": "NA",
    "continent_name": "North America",
    "country_code2": "US",
    "country_code3": "USA",
    "country_name": "United States",
    "country_capital": "Washington, D.C.",
    "state_prov": "California",
    "district": "",
    "city": "Los Angeles",
    "zipcode": "90012",
    "latitude": "34.05361",
    "longitude": "-118.24550",
    "geoname_id": "5332870",
    "is_eu": false,
    "calling_code": "+1",
    "country_tld": ".us",
    "languages": "en-US,es-US,haw,fr",
    "country_flag": "https://ipgeolocation.io/static/flags/us_64.png",
    "isp": "APNIC Research and Development",
    "connection_type": "",
    "organization": "Cloudflare, Inc.",
    "asn": "AS13335",
    "currency": {
      "code": "USD",
      "name": "US Dollar",
      "symbol": "$"
    },
    "time_zone": {
      "name": "America/Los_Angeles",
      "offset": -8,
      "current_time": "2025-02-10 04:04:17.219-0800",
      "current_time_unix": 1739189057,
      "is_dst": false,
      "dst_savings": 0
    }
  }
]
```
</details>

### DB-V (IP TO PROXY)

#### Overview
Provides security-related insights by identifying proxy usage, VPNs, Tor nodes, and other anonymization techniques. Includes a threat score and classifications for known attackers, cloud providers, and spam activity.
```json
{
  "ip": "1.0.0.0",
  "continent_code": "OC",
  "continent_name": "Oceania",
  "country_code2": "AU",
  "country_code3": "AUS",
  "country_name": "Australia",
  "country_capital": "Canberra",
  "is_eu": false,
  "calling_code": "+61",
  "country_tld": ".au",
  "languages": "en-AU",
  "country_flag": "https://ipgeolocation.io/static/flags/au_64.png",
  "isp": "APNIC Research and Development",
  "connection_type": "",
  "organization": "Cloudflare, Inc.",
  "asn": "AS13335",
  "currency": {
    "code": "AUD",
    "name": "Australian Dollar",
    "symbol": "A$"
  },
  "security": {
    "threat_score": 80,
    "is_tor": "false",
    "is_proxy": "true",
    "proxy_type": "VPN",
    "is_anonymous": "true",
    "is_known_attacker": "true",
    "is_cloud_provider": "true",
    "is_bot": "false",
    "is_spam": "false"
  }
}
```
#### For Bulk lookup

Sample Response for Two IPs:
<details>
<summary>Click to expand response</summary>

```json
[
  {
    "ip": "1.0.0.0",
    "continent_code": "OC",
    "continent_name": "Oceania",
    "country_code2": "AU",
    "country_code3": "AUS",
    "country_name": "Australia",
    "country_capital": "Canberra",
    "is_eu": false,
    "calling_code": "+61",
    "country_tld": ".au",
    "languages": "en-AU",
    "country_flag": "https://ipgeolocation.io/static/flags/au_64.png",
    "isp": "APNIC Research and Development",
    "connection_type": "",
    "organization": "Cloudflare, Inc.",
    "asn": "AS13335",
    "currency": {
      "code": "AUD",
      "name": "Australian Dollar",
      "symbol": "A$"
    },
    "security": {
      "threat_score": 80,
      "is_tor": "false",
      "is_proxy": "true",
      "proxy_type": "VPN",
      "is_anonymous": "true",
      "is_known_attacker": "true",
      "is_cloud_provider": "true",
      "is_bot": "false",
      "is_spam": "false"
    }
  },
  {
    "ip": "1.0.0.1",
    "continent_code": "NA",
    "continent_name": "North America",
    "country_code2": "US",
    "country_code3": "USA",
    "country_name": "United States",
    "country_capital": "Washington, D.C.",
    "is_eu": false,
    "calling_code": "+1",
    "country_tld": ".us",
    "languages": "en-US,es-US,haw,fr",
    "country_flag": "https://ipgeolocation.io/static/flags/us_64.png",
    "isp": "APNIC Research and Development",
    "connection_type": "",
    "organization": "Cloudflare, Inc.",
    "asn": "AS13335",
    "currency": {
      "code": "USD",
      "name": "US Dollar",
      "symbol": "$"
    },
    "security": {
      "threat_score": 80,
      "is_tor": "false",
      "is_proxy": "true",
      "proxy_type": "VPN",
      "is_anonymous": "true",
      "is_known_attacker": "true",
      "is_cloud_provider": "true",
      "is_bot": "false",
      "is_spam": "false"
    }
  }
]
```
</details>
        
### DB-VI (IP TO CITY+PROXY)

#### Overview
Combines detailed city-level geolocation with security insights, detecting proxy usage, VPNs, and known threats. Provides latitude, longitude, time zone, and risk classification data for enhanced cybersecurity and location intelligence.
```json
{
  "ip": "1.0.0.0",
  "continent_code": "OC",
  "continent_name": "Oceania",
  "country_code2": "AU",
  "country_code3": "AUS",
  "country_name": "Australia",
  "country_capital": "Canberra",
  "state_prov": "Queensland",
  "district": "",
  "city": "Brisbane",
  "zipcode": "4101",
  "latitude": "-27.47306",
  "longitude": "153.01421",
  "geoname_id": "10113228",
  "is_eu": false,
  "calling_code": "+61",
  "country_tld": ".au",
  "languages": "en-AU",
  "country_flag": "https://ipgeolocation.io/static/flags/au_64.png",
  "currency": {
    "code": "AUD",
    "name": "Australian Dollar",
    "symbol": "A$"
  },
  "time_zone": {
    "name": "Australia/Brisbane",
    "offset": 10,
    "current_time": "2025-02-10 20:35:13.983+1000",
    "current_time_unix": 1739183713,
    "is_dst": false,
    "dst_savings": 0
  },
  "security": {
    "threat_score": 75,
    "is_tor": "false",
    "is_proxy": "true",
    "proxy_type": "VPN",
    "is_anonymous": "true",
    "is_known_attacker": "true",
    "is_bot": "false",
    "is_spam": "false"
  }
}
```
#### For Bulk lookup

Sample Response for Two IPs:
<details>
<summary>Click to expand response</summary>

```json
[
  {
    "ip": "1.0.0.0",
    "continent_code": "OC",
    "continent_name": "Oceania",
    "country_code2": "AU",
    "country_code3": "AUS",
    "country_name": "Australia",
    "country_capital": "Canberra",
    "state_prov": "Queensland",
    "district": "",
    "city": "Brisbane",
    "zipcode": "4101",
    "latitude": "-27.47306",
    "longitude": "153.01421",
    "geoname_id": "10113228",
    "is_eu": false,
    "calling_code": "+61",
    "country_tld": ".au",
    "languages": "en-AU",
    "country_flag": "https://ipgeolocation.io/static/flags/au_64.png",
    "currency": {
      "code": "AUD",
      "name": "Australian Dollar",
      "symbol": "A$"
    },
    "time_zone": {
      "name": "Australia/Brisbane",
      "offset": 10,
      "current_time": "2025-02-10 21:57:00.643+1000",
      "current_time_unix": 1739188620,
      "is_dst": false,
      "dst_savings": 0
    },
    "security": {
      "threat_score": 75,
      "is_tor": "false",
      "is_proxy": "true",
      "proxy_type": "VPN",
      "is_anonymous": "true",
      "is_known_attacker": "true",
      "is_bot": "false",
      "is_spam": "false"
    }
  },
  {
    "ip": "1.0.0.1",
    "continent_code": "NA",
    "continent_name": "North America",
    "country_code2": "US",
    "country_code3": "USA",
    "country_name": "United States",
    "country_capital": "Washington, D.C.",
    "state_prov": "California",
    "district": "",
    "city": "Los Angeles",
    "zipcode": "90012",
    "latitude": "34.05361",
    "longitude": "-118.24550",
    "geoname_id": "5332870",
    "is_eu": false,
    "calling_code": "+1",
    "country_tld": ".us",
    "languages": "en-US,es-US,haw,fr",
    "country_flag": "https://ipgeolocation.io/static/flags/us_64.png",
    "currency": {
      "code": "USD",
      "name": "US Dollar",
      "symbol": "$"
    },
    "time_zone": {
      "name": "America/Los_Angeles",
      "offset": -8,
      "current_time": "2025-02-10 03:57:00.651-0800",
      "current_time_unix": 1739188620,
      "is_dst": false,
      "dst_savings": 0
    },
    "security": {
      "threat_score": 75,
      "is_tor": "false",
      "is_proxy": "true",
      "proxy_type": "VPN",
      "is_anonymous": "true",
      "is_known_attacker": "true",
      "is_bot": "false",
      "is_spam": "false"
    }
  }
]
```
</details>

### DB-VII (IP TO CITY+ISP+PROXY)

#### Overview
Combines detailed city-level geolocation with ISP and security insights, detecting proxy usage, VPNs, and known threats. Provides latitude, longitude, time zone, ASN, organization, and risk classification data for enhanced cybersecurity, network analysis, and location intelligence.
```json
{
    "ip": "1.0.0.0",
    "continent_code": "OC",
    "continent_name": "Oceania",
    "country_code2": "AU",
    "country_code3": "AUS",
    "country_name": "Australia",
    "country_capital": "Canberra",
    "state_prov": "Queensland",
    "district": "",
    "city": "Brisbane",
    "zipcode": "4101",
    "latitude": "-27.47306",
    "longitude": "153.01421",
    "geoname_id": "10113228",
    "is_eu": false,
    "calling_code": "+61",
    "country_tld": ".au",
    "languages": "en-AU",
    "country_flag": "https://ipgeolocation.io/static/flags/au_64.png",
    "isp": "APNIC Research and Development",
    "connection_type": "",
    "organization": "Cloudflare, Inc.",
    "asn": "AS13335",
    "currency": {
        "code": "AUD",
        "name": "Australian Dollar",
        "symbol": "A$"
    },
    "time_zone": {
        "name": "Australia/Brisbane",
        "offset": 10,
        "current_time": "2025-02-10 20:37:26.443+1000",
        "current_time_unix": 1739183846,
        "is_dst": false,
        "dst_savings": 0
    },
    "security": {
        "threat_score": 80,
        "is_tor": "false",
        "is_proxy": "true",
        "proxy_type": "VPN",
        "is_anonymous": "true",
        "is_known_attacker": "true",
        "is_cloud_provider": "true",
        "is_bot": "false",
        "is_spam": "false"
    }
}
```

#### For Bulk lookup
Sample Response for Two IPs:
<details>
<summary>Click to expand response</summary>

```json
[
  {
    "ip": "1.0.0.0",
    "continent_code": "OC",
    "continent_name": "Oceania",
    "country_code2": "AU",
    "country_code3": "AUS",
    "country_name": "Australia",
    "country_capital": "Canberra",
    "state_prov": "Queensland",
    "district": "",
    "city": "Brisbane",
    "zipcode": "4101",
    "latitude": "-27.47306",
    "longitude": "153.01421",
    "geoname_id": "10113228",
    "is_eu": false,
    "calling_code": "+61",
    "country_tld": ".au",
    "languages": "en-AU",
    "country_flag": "https://ipgeolocation.io/static/flags/au_64.png",
    "isp": "APNIC Research and Development",
    "connection_type": "",
    "organization": "Cloudflare, Inc.",
    "asn": "AS13335",
    "currency": {
      "code": "AUD",
      "name": "Australian Dollar",
      "symbol": "A$"
    },
    "time_zone": {
      "name": "Australia/Brisbane",
      "offset": 10,
      "current_time": "2025-02-10 21:53:45.918+1000",
      "current_time_unix": 1739188425,
      "is_dst": false,
      "dst_savings": 0
    },
    "security": {
      "threat_score": 80,
      "is_tor": "false",
      "is_proxy": "true",
      "proxy_type": "VPN",
      "is_anonymous": "true",
      "is_known_attacker": "true",
      "is_cloud_provider": "true",
      "is_bot": "false",
      "is_spam": "false"
    }
  },
  {
    "ip": "1.0.0.1",
    "continent_code": "NA",
    "continent_name": "North America",
    "country_code2": "US",
    "country_code3": "USA",
    "country_name": "United States",
    "country_capital": "Washington, D.C.",
    "state_prov": "California",
    "district": "",
    "city": "Los Angeles",
    "zipcode": "90012",
    "latitude": "34.05361",
    "longitude": "-118.24550",
    "geoname_id": "5332870",
    "is_eu": false,
    "calling_code": "+1",
    "country_tld": ".us",
    "languages": "en-US,es-US,haw,fr",
    "country_flag": "https://ipgeolocation.io/static/flags/us_64.png",
    "isp": "APNIC Research and Development",
    "connection_type": "",
    "organization": "Cloudflare, Inc.",
    "asn": "AS13335",
    "currency": {
      "code": "USD",
      "name": "US Dollar",
      "symbol": "$"
    },
    "time_zone": {
      "name": "America/Los_Angeles",
      "offset": -8,
      "current_time": "2025-02-10 03:53:45.924-0800",
      "current_time_unix": 1739188425,
      "is_dst": false,
      "dst_savings": 0
    },
    "security": {
      "threat_score": 80,
      "is_tor": "false",
      "is_proxy": "true",
      "proxy_type": "VPN",
      "is_anonymous": "true",
      "is_known_attacker": "true",
      "is_cloud_provider": "true",
      "is_bot": "false",
      "is_spam": "false"
    }
  }
]
```
</details>
