package io.ipgeolocation.databaseReader.databases.place

import com.maxmind.db.MaxMindDbConstructor
import com.maxmind.db.MaxMindDbParameter
import groovy.transform.CompileStatic
import org.springframework.util.Assert

import static com.google.common.base.Strings.nullToEmpty

@CompileStatic
class Place {
    final Integer id
    private final String nameEnglish
    private final String nameGerman
    private final String nameRussian
    private final String nameJapanese
    private final String nameFrench
    private final String nameChinese
    private final String nameSpanish
    private final String nameCzech
    private final String nameItalian

    Place(Integer id, String nameEnglish, String nameGerman, String nameRussian, String nameJapanese, String nameFrench,
          String nameChinese, String nameSpanish, String nameCzech, String nameItalian) {
        Assert.notNull(id, "'id' must not be null.")
        Assert.hasText(nameEnglish, "'nameEnglish' must not be empty or null.")

        this.id = id
        this.nameEnglish = nameEnglish
        this.nameGerman = nullToEmpty(nameGerman)
        this.nameRussian = nullToEmpty(nameRussian)
        this.nameJapanese = nullToEmpty(nameJapanese)
        this.nameFrench = nullToEmpty(nameFrench)
        this.nameChinese = nullToEmpty(nameChinese)
        this.nameSpanish = nullToEmpty(nameSpanish)
        this.nameCzech = nullToEmpty(nameCzech)
        this.nameItalian = nullToEmpty(nameItalian)
    }

    @MaxMindDbConstructor
    Place(@MaxMindDbParameter(name = "en") String nameEnglish,
          @MaxMindDbParameter(name = "de") String nameGerman,
          @MaxMindDbParameter(name = "ru") String nameRussian,
          @MaxMindDbParameter(name = "ja") String nameJapanese,
          @MaxMindDbParameter(name = "fr") String nameFrench,
          @MaxMindDbParameter(name = "zh") String nameChinese,
          @MaxMindDbParameter(name = "es") String nameSpanish,
          @MaxMindDbParameter(name = "cs") String nameCzech,
          @MaxMindDbParameter(name = "it") String nameItalian) {
        this(-1, nameEnglish, nameGerman, nameRussian, nameJapanese, nameFrench, nameChinese, nameSpanish, nameCzech,
                nameItalian)
    }

    String getName(String lang = "en") {
        String name = nameEnglish

        switch(lang) {
            case "en":
                name = nameEnglish
                break
            case "de":
                name = nameGerman
                break
            case "ru":
                name = nameRussian
                break
            case "ja":
                name = nameJapanese
                break
            case "fr":
                name = nameFrench
                break
            case "cn":
                name = nameChinese
                break
            case "es":
                name = nameSpanish
                break
            case "cs":
                name = nameCzech
                break
            case "it":
                name = nameItalian
                break
        }

        name
    }
}
