package me.voltbox.hackergy.enriching.enrichments;

import me.voltbox.hackergy.common.domain.GrantDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CategoryMappingService {
    public List<String> mapCategory(GrantDto grantDto) {

        var list = new ArrayList<String>();
        if ("BMWI".equals(grantDto.getSource())) {
            for (var category : grantDto.getCategory()) {
                list.add(mapBmwi(category));
            }
        } else if ("ItFoerderungen".equals(grantDto.getSource())) {
            for (var category : grantDto.getCategory()) {
                list.add(mapItFoerderungen(category));
            }
        } else if ("EnergieagenturRLP".equals(grantDto.getSource())) {
            list.add("Arbeiten");
        }

        if (list.size() > 1) {
            list.remove("Andere");
        }

        return list.isEmpty() ? List.of("Andere") : list;
    }

    private String mapItFoerderungen(String category) {
        return switch (category.trim()) {
            case "Weiterbildung in digitalen Themen" -> "Lernen";
            case "Andere" -> "Beraten";
            default -> "Digitalisieren";
        };
    }

    private String mapBmwi(String category) {
        return switch (category.trim()) {
            case "Arbeit" -> "Arbeiten";
            case "Aus- & Weiterbildung", "Außenwirtschaft" -> "Lernen";
            case "Beratung", "Corona-Hilfe" -> "Beraten";
            case "Energieeffizienz & Erneuerbare Energien" -> "Sparen";
            case "Existenzgründung & -festigung" -> "Gründen";
            case "Kultur, Medien & Sport", "Gesundheit & Soziales", "Regionalförderung", "Forschung & Innovation(themenoffen)",
                    "Forschung & Innovation (themenspezifisch)", "Landwirtschaft & Ländliche Entwicklung" -> "Fördern";
            case "Messen & Ausstellungen" -> "Werben";
            case "Umwelt- & Naturschutz", "Unternehmensfinanzierung" -> "Schützen";
            case "Infrastruktur", "Wohnungsbau & Modernisierung", "Städtebau & Stadterneuerung" -> "Bauen";
            case "Smart Cities & Regionen", "Digitalisierung" -> "Digitalisieren";
            default -> "Andere";
        };
    }
}
