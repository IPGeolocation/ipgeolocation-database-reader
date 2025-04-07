package io.ipgeolocation.databaseReader.databases.cloudprovider

import groovy.transform.CompileStatic
import me.xdrop.fuzzywuzzy.FuzzySearch
import static com.google.common.base.Preconditions.checkNotNull

@CompileStatic
class CloudProviderIndexer {
    private Set<String> cloudProviders
    private Set<String> cloudAsnSet
    private Set<String> backupCloudAsnSet
    public static volatile boolean updatingMainCache = false
    CloudProviderIndexer() {
        cloudProviders = new HashSet<String>()
        cloudAsnSet = new HashSet<String>()
        backupCloudAsnSet = new HashSet<String>()
    }

    void index(String cloudProvider) {
        checkNotNull(cloudProvider, "Pre-condition violated: cloud provider must not be null.")
        cloudProvider = normalizeOrganisation(cloudProvider)

        cloudProviders.add(cloudProvider)
    }

    Boolean isCloudProvider(String ispOrOrg) {
        checkNotNull(ispOrOrg, "Pre-condition violated: cloud provider must not be null.")
        ispOrOrg = normalizeOrganisation(ispOrOrg)
        cloudProviders.contains(ispOrOrg)
    }

    private static String normalizeOrganisation(String org) {
        org.toLowerCase().replaceAll("[\\s.,\\-_\\\"]", "")
    }

    private static Boolean matches(String org1, String org2) {
        int score = FuzzySearch.ratio(org1, org2)
        score >= 95
    }

    void indexCloudAsn(String cloudAsn) {
        cloudAsn = normalizeAsn(cloudAsn)
        cloudAsnSet.add(cloudAsn)
    }

    boolean isCloudAsn(String asn) {
        checkNotNull(asn, "Pre-condition violated: asn must not be null.")
        asn = normalizeAsn(asn)
        if (updatingMainCache) {
            return backupCloudAsnSet.contains(asn)
        } else {
            return cloudAsnSet.contains(asn)
        }
    }

    private static String normalizeAsn(String asn) {
        asn.toLowerCase().replaceAll("as|[\\s.,\\-_\\\"]", "")
    }

    void copyMainToBackupCache() {
        backupCloudAsnSet.addAll(cloudAsnSet)
    }

    Integer size() {
        cloudProviders.size()
    }

    Integer sizeCloudAsn() {
        cloudAsnSet.size()
    }

    void clearCloudASNSet () {
        cloudAsnSet.clear()
    }

    void clearBackupCloudASNSet () {
        backupCloudAsnSet.clear()
    }
}
