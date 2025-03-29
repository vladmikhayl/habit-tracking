package com.vladmikhayl.report.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReportPhotoEditingRequest {

    // Чтобы положить значение null в поле photoUrl, в качестве этого параметра нужно передать пустую строку

    private String photoUrl;

}
