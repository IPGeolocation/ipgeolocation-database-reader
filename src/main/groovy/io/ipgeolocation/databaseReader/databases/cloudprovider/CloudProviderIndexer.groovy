package io.ipgeolocation.databaseReader.databases.cloudprovider

import groovy.transform.CompileStatic

import static com.google.common.base.Preconditions.checkNotNull

@CompileStatic
class CloudProviderIndexer {
    private Set<String> cloudProviders

    CloudProviderIndexer() {
        cloudProviders = new HashSet<String>()
    }

    void index(String cloudProvider) {
        checkNotNull(cloudProvider, "Pre-condition violated: cloud provider must not be null.")

        cloudProviders.add(cloudProvider)
    }

    Boolean isCloudProvider(String cloudProvider) {
        checkNotNull(cloudProvider, "Pre-condition violated: cloud provider must not be null.")

        cloudProviders.contains(cloudProvider)
    }

    Integer size() {
        cloudProviders.size()
    }
}
