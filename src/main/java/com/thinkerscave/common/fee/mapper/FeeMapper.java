package com.thinkerscave.common.fee.mapper;

import com.thinkerscave.common.fee.domain.FeeStructure;
import com.thinkerscave.common.fee.domain.FeeStructureItem;
import com.thinkerscave.common.fee.dto.FeeStructureDTO;
import com.thinkerscave.common.fee.dto.FeeStructureItemDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper
public interface FeeMapper {

    @Mapping(target = "items", ignore = true)
    FeeStructureDTO toFeeStructureDto(FeeStructure structure);

    FeeStructureItemDTO toItemDto(FeeStructureItem item);

    List<FeeStructureItemDTO> toItemDtoList(List<FeeStructureItem> items);

    FeeStructureItem toItemEntity(FeeStructureItemDTO dto);
}
