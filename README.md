# Replacing TLS material for spring boot applications at runtime

This repo provides three modules that you can use in order to allow the replacement of TLS certificates and keys at runtime for spring boot applications:

1. ```jetty``` - enables you to update the server certificate and key of a spring boot application.
2. ```rest-template``` - provides a custom rest template that allows to update its trust certificate at runtime.
3. ```cloud-config-client``` - if you are using the spring cloud config server you can use this module to configure the rest template of module 2 in order to update its trust.

The other modules are there to show how to use them:

1. ```cloud-config-server``` - starts a spring cloud config server used by the hello-world service.
2. ```message-service``` - a little rest controller that the hello service uses to demonstrate how to update the trust of a RestTemplate at runtime.
3. ```hello-service``` - a little rest service that binds all of this together: Dynamically update the servers TLS material and the rest templates TLS material. 

All of this is completely based on the work of [Hakky54](https://github.com/Hakky54) at https://github.com/Hakky54/sslcontext-kickstart. A great place to lok at when it comes to TLS and Java.

## Table of contents
1. [Introduction](#introduction)
2. [Usage](#usage)
   1. [Configuration](#configuration)
   2. [Renew TLS material](#renew-tls-material)
   3. [Cloud config](#cloud-config)

## Introduction
Nowadays in the world of containerized microservices, there is rarely a need of working with TLS encryption directly in your java program code. But if you have to and if you are developing spring boot applications, then this repo might give you an idea of how to do it.

For a spring boot application there are two different places that need to be addressed:

1. The server certificate of the application itself and
2. the client trust of any ```RestTemplate``` that is used to connect to other services.

In addition, if you are using spring cloud config for your spring configuration, there is a third place where you might want to update TLS material at runtime.

This repo shows how it can be done. It provides a rudimentary way of updating TLS material. But it might serve as a starting point.

## Usage
### Configuration
All three modules are programmed to be autoconfigured when included as dependency into a spring boot application. The only thing you need to know is that all this only works if you use the Jetty server instead of the default Tomcat. 

Take a look into the [build.gradle](hello-world/build.gradle) of the ```hello-world``` service to see how it is done in gradle. 

Currently, the libraries only support *server* certificates so there are only three properties that need to be given in order to tell the library where to find the TLS material:

```
neubert.tobias.tls:
  identity-cert-resource: file:/etc/something/certificate.pem
  identity-key-resource: file/etc/someting/private.key
  trust-resource: file:/etc/something/certificate.pem
```

The three files can be created like this, where the certificates have to be given as ```PEM``` files. Take a look into the library of [Hakky54](https://github.com/Hakky54/sslcontext-kickstart) in order to learn how to use different formats. If you like, you can of course use the default java keystores, which I don't like that much.

```
openssl req -x509 -sha256 -nodes -days 36500 -newkey rsa:4096 -keyout private.key -out certificate.pem
```

This command creates a self signed certificate for testing purposes only.

### Renew TLS material
Renewing the TLS material in this library is done by a ```java.nio.file.WatchService```. It is a very basic one that simply listens on changes of the configured ```identity-cert-resource```, ```identity-key-resource``` for a server and ```trust-resource``` for a rest template. 

Simply overwrite the current certificates and keys with new ones. The ```FileBasedTlsUpdateService``` and the ```DynamicTlsRestTemplate``` will recognize the new files and replace the old with the new ones.

**Important: For the server, always overwrite the private key first and the certificate second.**

Otherwise, the ```FileBasedTlsUpdateService``` would try to use the old private key together with the new certificate which won't work. 

The ```neubert.tobias.jetty.tls.FileBasedTlsUpdateServiceTest``` demonstrates how to do it.

### Cloud config
In order to inject the ```DynamicTlsRestTemplate``` into the cloud config client, the ```cloud-config-client``` module of this repo configures a ```org.springframework.cloud.config.client.ConfigServicePropertySourceLocator``` to use the ```DynamicTlsRestTemplate``` instead of springs default one. As described before, it has to be configured with spring properties. I didn't find a way of doing that wihtin the normal application context. Instead I am doing this as part of the bootstrap context which is not used anymore by default. 

So in order to work with the solution described here, you habe to enable it by providing the property ```spring.cloud.bootstrap.enabled=true``` at startup of your application. Take a look into the ```hello-world``` service of how it is done there.

