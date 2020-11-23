## Description

This repository contains a nuxeo liveconnect plugin for the [Microsoft Graph API](https://developer.microsoft.com/en-us/graph) using [Microsoft's Java SDK](https://github.com/microsoftgraph/msgraph-sdk-java)

## How to build
```
git clone https://github.com/nuxeo-sandbox/nuxeo-liveconnect-onedrive-graph
cd nuxeo-liveconnect-onedrive-graph
mvn clean install
```

## Azure Configuration

* Log into the [azure portal](https://portal.azure.com/#home) with your microsoft account
* Click on App registrations
* Click on Register a new application
    * Set a name
    * Choose "Accounts in any organizational directory (Any Azure AD directory - Multitenant) and personal Microsoft accounts (e.g. Skype, Xbox)"
    * Set the redirect URI to http(s)://MY_SERVER/nuxeo/site/oauth2/msgraph/callback
    * Click on Create
* In the Authentication menu
    * Add a second redirect URI http(s)://MY_SERVER/nuxeo/ui/nuxeo-liveconnect/nuxeo-liveconnect-onedrive-picker.html
    * Check Access tokens and ID tokens in Implicit grant
* In the Certificates & secrets menu, create a secret and copy the value
* In the API permissions, add permissions (Microsoft Graph / Delegated permissions)
    * offline_access
    * Files.ReadWrite.All

## Nuxeo Configuration
* log into Nuxeo as an administrator
* Go to Administration / Cloud Services in the left drawer menu
* Edit the msgraph service, set the client id and secret, then check enabled



## Known limitations
This plugin is a work in progress.

## About Nuxeo
[Nuxeo](www.nuxeo.com), developer of the leading Content Services Platform, is reinventing enterprise content management (ECM) and digital asset management (DAM). Nuxeo is fundamentally changing how people work with data and content to realize new value from digital information. Its cloud-native platform has been deployed by large enterprises, mid-sized businesses and government agencies worldwide. Customers like Verizon, Electronic Arts, ABN Amro, and the Department of Defense have used Nuxeo's technology to transform the way they do business. Founded in 2008, the company is based in New York with offices across the United States, Europe, and Asia.

Learn more at www.nuxeo.com.
