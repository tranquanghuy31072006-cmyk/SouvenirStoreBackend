package com.learning.souvenirstorebackend.service;

import com.learning.souvenirstorebackend.dto.request.BrandRequest;
import com.learning.souvenirstorebackend.dto.response.BrandResponse;
import com.learning.souvenirstorebackend.entity.Brand;
import com.learning.souvenirstorebackend.enums.BrandStatus;
import com.learning.souvenirstorebackend.exception.AppException;
import com.learning.souvenirstorebackend.exception.ErrorCode;
import com.learning.souvenirstorebackend.mapper.BrandMapper;
import com.learning.souvenirstorebackend.repository.BrandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BrandServiceTest {

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private BrandMapper brandMapper;

    @InjectMocks
    private BrandService brandService;

    private Brand brand;
    private BrandRequest request;
    private BrandResponse response;

    @BeforeEach
    public void setUp() {
        brand = Brand.builder().id(1L).name("TestBrand").status(BrandStatus.ACTIVE).build();
        request = BrandRequest.builder().name("TestBrand").build();
        response = BrandResponse.builder().id(1L).name("TestBrand").build();
    }

    @Test
    void createBrand_whenNameNotExists_returnBrandResponse(){
        when(brandRepository.existsByName(anyString())).thenReturn(false);

        when(brandMapper.toBrand(request)).thenReturn(brand);
        when(brandRepository.save(brand)).thenReturn(brand);
        when(brandMapper.toBrandResponse(brand)).thenReturn(response);

        BrandResponse result = brandService.createBrand(request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("TestBrand");
    }

    @Test
    void createBrand_whenNameExists_returnBrandResponse(){
        when(brandRepository.existsByName(anyString())).thenReturn(true);

        assertThatThrownBy(() -> brandService.createBrand(request)).isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BRAND_ALREADY_EXISTS);

        verify(brandRepository, never()).save(any());
    }

    @Test
    void getBrandById_whenIdExists_returnBrand(){
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(brandMapper.toBrandResponse(brand)).thenReturn(response);

        BrandResponse result = brandService.getBrandById(1L);
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getBrandById_whenNotFound_throwException(){
        when(brandRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> brandService.getBrandById(1L)).isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BRAND_NOT_FOUND);
    }

    @Test
    void activateBrand_whenInactive_setsActiveAndSaves(){
        brand.setStatus(BrandStatus.INACTIVE);

        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(brandRepository.save(any(Brand.class))).thenReturn(brand);

        brandService.activateBrand(1L);
        assertThat(brand.getStatus()).isEqualTo(BrandStatus.ACTIVE);

        verify(brandRepository, times(1)).findById(1L);
    }

    @Test
    void activateBrand_whenActive_throwException(){
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

        assertThatThrownBy(() -> brandService.activateBrand(1L)).isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BRAND_ALREADY_ACTIVE);

        verify(brandRepository,never()).save(any());
    }

    @Test
    void createBrand_whenSaveThrowsDataIntegrityViolation_throwBrandAlreadyExists(){
        when(brandRepository.existsByName(anyString())).thenReturn(false);

        when(brandMapper.toBrand(request)).thenReturn(brand);

        when(brandRepository.save(any(Brand.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        assertThatThrownBy(() -> brandService.createBrand(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BRAND_ALREADY_EXISTS);

        verify(brandRepository, times(1)).save(any());
    }

    @Test
    void getActiveBrands_whenHaveActiveBrands_thenReturnMappedList(){}

    @Test
    void updateBrand_whenExists_thenCallsMapperUpdateAndSave(){
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(brandRepository.save(brand)).thenReturn(brand);
        when(brandMapper.toBrandResponse(brand)).thenReturn(response);

        var result = brandService.updateBrand(1L, request);

        assertThat(result.getName()).isEqualTo("TestBrand");

        verify(brandMapper).updateBrand(brand, request);
        verify(brandRepository).save(brand);
    }

    @Test
    void updateBrand_whenNotFound_throwsBrandNotFound() {
        when(brandRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> brandService.updateBrand(1L, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BRAND_NOT_FOUND);

        verify(brandRepository, never()).save(any());
    }

    @Test
    void deactivateBrand_whenActive_setsInactiveAndSaves(){
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

        brandService.deactivateBrand(1L);

        assertThat(brand.getStatus()).isEqualTo(BrandStatus.INACTIVE);

        verify(brandRepository, times(1)).save(brand);
    }

    @Test
    void deactivateBrand_whenAlreadyInactive_throwsBrandAlreadyInactive(){
        brand.setStatus(BrandStatus.INACTIVE);

        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

        assertThatThrownBy(() -> brandService.deactivateBrand(1L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BRAND_ALREADY_INACTIVE);

        verify(brandRepository,never()).save(any());
    }

    @Test
    void deactivateBrand_whenNotFound_throwsBrandNotFound(){
        when(brandRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> brandService.deactivateBrand(1L)).isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BRAND_NOT_FOUND);
    }

    @Test
    void deleteBrand_whenExists_callsDelete() {
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

        brandService.deleteBrand(1L);

        verify(brandRepository, times(1)).delete(brand);
    }

    @Test
    void deleteBrand_whenNotFound_throwsBrandNotFound(){
        when(brandRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> brandService.deleteBrand(1L)).isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BRAND_NOT_FOUND);

        verify(brandRepository,never()).delete(any());
    }
}
