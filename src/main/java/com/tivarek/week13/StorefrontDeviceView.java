package com.tivarek.week13;

import java.util.List;

public record StorefrontDeviceView(StorefrontProductView product, String description,
                                  String inspectionStatus, List<Spec> specifications, List<Photo> photos) {
    public record Spec(String name, String value, String unit) { }
    public record Photo(String url, String alt) { }
}
