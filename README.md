# Description

This repository contains a Nuxeo Live Connect plugin for the [Microsoft Graph API](https://developer.microsoft.com/en-us/graph) using [Microsoft's Java SDK](https://github.com/microsoftgraph/msgraph-sdk-java). This restores the ability to use Nuxeo Live Connect with OneDrive.

# How to build

```
git clone https://github.com/nuxeo-sandbox/nuxeo-liveconnect-onedrive-graph
cd nuxeo-liveconnect-onedrive-graph
mvn clean install
```

# Azure Configuration

* Log into the [Azure portal](https://portal.azure.com/#home) with your Microsoft account
* Use the search bar to locate the "App registrations" service
* Click **New registration**
    * Set a value for **Name**; this is the display name shown in the OAuth dialog so choose something meaningful
    * For **Supported account types** choose `Accounts in any organizational directory (Any Azure AD directory - Multitenant) and personal Microsoft accounts (e.g. Skype, Xbox)`
    * Set the **Redirect URI** to `https://<your_SERVER>/nuxeo/site/oauth2/msgraph/callback`
    * Click on **Register**
    * Make note of the **Application (client) ID**; this will be used in Nuxeo later
* Open the **Authentication** menu
    * Add a second redirect URI `https://<your_SERVER>/nuxeo/ui/nuxeo-liveconnect/nuxeo-liveconnect-onedrive-picker.html`
    * Check `Access tokens` and `ID tokens` under **Implicit grant**
* Open the **Certificates & secrets** menu
  * Create a **New client secret**
  * Copy the **Value**; this will be used in Nuxeo later
  * Note: do *not* copy the **ID**, this is just the UUID of the secret record
* Open the **API permissions** menu
  * Click on **Add permission**
    * Choose `Microsoft Graph`
    * Choose `Delegated permissions`
    * Enable `OpenId permissions > offline_access`
    * Enable `Files > Files.ReadWrite.All`

# Nuxeo Configuration

* Log into Nuxeo as an administrator
* Go to **Administration > Cloud Services** in the left drawer menu
* Edit the `msgraph` service, set the **Client ID** and **Client Secret** to the values copied in the preceding steps
* Check `Enabled`
* Click **Save**

# Support

**These features are not part of the Nuxeo Production platform.**

These solutions are provided for inspiration and we encourage customers to use them as code samples and learning resources.

This is a moving project (no API maintenance, no deprecation process, etc.) If any of these solutions are found to be useful for the Nuxeo Platform in general, they will be integrated directly into platform, not maintained here.

# License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)

# About Nuxeo

Nuxeo Platform is an open source Content Services platform, written in Java. Data can be stored in both SQL & NoSQL databases.

The development of the Nuxeo Platform is mostly done by Nuxeo employees with an open development model.

The source code, documentation, roadmap, issue tracker, testing, benchmarks are all public.

Typically, Nuxeo users build different types of information management solutions for [document management](https://www.nuxeo.com/solutions/document-management/), [case management](https://www.nuxeo.com/solutions/case-management/), and [digital asset management](https://www.nuxeo.com/solutions/dam-digital-asset-management/), use cases. It uses schema-flexible metadata & content models that allows content to be repurposed to fulfill future use cases.

More information is available at [www.nuxeo.com](https://www.nuxeo.com).
