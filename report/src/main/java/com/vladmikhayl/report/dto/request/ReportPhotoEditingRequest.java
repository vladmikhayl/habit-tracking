package com.vladmikhayl.report.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Запрос на изменение фото в отчете")
public class ReportPhotoEditingRequest {

    // Чтобы положить значение null в поле photoUrl, в качестве этого параметра нужно передать пустую строку

    @Schema(description = "Новое URL фото", example = "https://i.pinimg.com/736x/b9/a7/55/b9a75516248779bead50d84c52daebf3.jpg")
    private String photoUrl;

}
