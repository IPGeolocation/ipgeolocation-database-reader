package io.ipgeolocation.databaseReader.databases.place

import groovy.transform.CompileStatic

import static com.google.common.base.Preconditions.checkNotNull
import static com.google.common.base.Strings.nullToEmpty

@CompileStatic
class Place {
    Integer id
    private String name_en
    private String name_de
    private String name_ru
    private String name_ja
    private String name_fr
    private String name_zh
    private String name_es
    private String name_cs
    private String name_it

    Place(Integer id, String name_en, String name_de, String name_ru, String name_ja, String name_fr, String name_zh, String name_es, String name_cs, String name_it) {
        checkNotNull(id, "Pre-condition violated: ID must not be null.")
        checkNotNull(name_en, "Pre-condition violated: name_en must not be null.")

        this.id = id
        this.name_en = name_en
        this.name_de = nullToEmpty(name_de)
        this.name_ru = nullToEmpty(name_ru)
        this.name_ja = nullToEmpty(name_ja)
        this.name_fr = nullToEmpty(name_fr)
        this.name_zh = nullToEmpty(name_zh)
        this.name_es = nullToEmpty(name_es)
        this.name_cs = nullToEmpty(name_cs)
        this.name_it = nullToEmpty(name_it)
    }

    String getName(String lang) {
        checkNotNull(lang, "Pre-condition violated: language must not be null.")

        String name

        switch(lang) {
            case "en":
                name = name_en
                break
            case "de":
                name = name_de
                break
            case "ru":
                name = name_ru
                break
            case "ja":
                name = name_ja
                break
            case "fr":
                name = name_fr
                break
            case "cn":
                name = name_zh
                break
            case "es":
                name = name_es
                break
            case "cs":
                name = name_cs
                break
            case "it":
                name = name_it
                break
            default:
                name = name_en
        }

        if (!name) {
            name = name_en
        }
        name
    }
}
