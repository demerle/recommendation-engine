package reccommendation_engine.RecEngine.mappers.impl;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import reccommendation_engine.RecEngine.domain.dto.imports.ImportResponseDto;
import reccommendation_engine.RecEngine.domain.dto.imports.ImportStatusDto;
import reccommendation_engine.RecEngine.domain.entities.IngestionRun;
import reccommendation_engine.RecEngine.mappers.ImportRunMapper;

@Component
public class ImportRunMapperImpl implements ImportRunMapper {

    private final ModelMapper modelMapper;

    public ImportRunMapperImpl(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    @Override
    public ImportResponseDto toImportResponseDto(IngestionRun ingestionRun) {
        ImportResponseDto responseDto = modelMapper.map(ingestionRun, ImportResponseDto.class);
        if (ingestionRun.getUser() != null) {
            responseDto.setUserId(ingestionRun.getUser().getId());
        }
        if (ingestionRun.getSource() != null) {
            responseDto.setSource(ingestionRun.getSource().name());
        }
        if (ingestionRun.getStatus() != null) {
            responseDto.setStatus(ingestionRun.getStatus().name());
        }
        responseDto.setIngestionRunId(ingestionRun.getId());
        return responseDto;
    }

    @Override
    public ImportStatusDto toImportStatusDto(IngestionRun ingestionRun) {
        ImportStatusDto statusDto = modelMapper.map(ingestionRun, ImportStatusDto.class);
        if (ingestionRun.getUser() != null) {
            statusDto.setUserId(ingestionRun.getUser().getId());
        }
        if (ingestionRun.getStatus() != null) {
            statusDto.setStatus(ingestionRun.getStatus().name());
        }
        statusDto.setIngestionRunId(ingestionRun.getId());
        return statusDto;
    }
}
