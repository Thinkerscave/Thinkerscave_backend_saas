package com.thinkerscave.documentation.controller;

import com.thinkerscave.documentation.entity.*;
import com.thinkerscave.documentation.service.DocumentationService;
import com.thinkerscave.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/documentation")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DocumentationController {

    private final DocumentationService service;

    @GetMapping("/students")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> students(@RequestParam Long classId) {
        return ResponseEntity.ok(ApiResponse.success("Class students", service.classStudents(classId)));
    }

    @GetMapping("/collection-masters")
    public ResponseEntity<ApiResponse<Page<DocCollectionMaster>>> collectionMasters(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Collection masters", service.searchCollectionMasters(status, keyword, pageable)));
    }

    @GetMapping("/collection-masters/approved")
    public ResponseEntity<ApiResponse<List<DocCollectionMaster>>> approvedCollections() {
        return ResponseEntity.ok(ApiResponse.success("Approved collection types", service.approvedCollectionMasters()));
    }

    @PostMapping("/collection-masters")
    public ResponseEntity<ApiResponse<DocCollectionMaster>> createCollectionMaster(@RequestBody DocCollectionMaster body) {
        return ResponseEntity.status(201).body(ApiResponse.created("Collection master saved", service.saveCollectionMaster(null, body)));
    }

    @PutMapping("/collection-masters/{id}")
    public ResponseEntity<ApiResponse<DocCollectionMaster>> updateCollectionMaster(@PathVariable Long id, @RequestBody DocCollectionMaster body) {
        return ResponseEntity.ok(ApiResponse.success("Collection master updated", service.saveCollectionMaster(id, body)));
    }

    @PostMapping("/collection-masters/{id}/submit")
    public ResponseEntity<ApiResponse<DocCollectionMaster>> submitCollection(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Sent to the principal", service.submitCollectionMaster(id)));
    }

    @PostMapping("/collection-masters/{id}/approve")
    public ResponseEntity<ApiResponse<DocCollectionMaster>> approveCollection(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Collection master approved", service.approveCollectionMaster(id)));
    }

    @PostMapping("/collection-masters/{id}/reject")
    public ResponseEntity<ApiResponse<DocCollectionMaster>> rejectCollection(@PathVariable Long id, @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.success("Collection master rejected", service.rejectCollectionMaster(id, reason)));
    }

    @GetMapping("/material-masters")
    public ResponseEntity<ApiResponse<Page<StudyMaterialMaster>>> materialMasters(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Material masters", service.searchMaterialMasters(status, keyword, pageable)));
    }

    @GetMapping("/material-masters/approved")
    public ResponseEntity<ApiResponse<List<StudyMaterialMaster>>> approvedMaterials() {
        return ResponseEntity.ok(ApiResponse.success("Approved material types", service.approvedMaterialMasters()));
    }

    @PostMapping("/material-masters")
    public ResponseEntity<ApiResponse<StudyMaterialMaster>> createMaterial(@RequestBody StudyMaterialMaster body) {
        return ResponseEntity.status(201).body(ApiResponse.created("Material master saved", service.saveMaterialMaster(null, body)));
    }

    @PutMapping("/material-masters/{id}")
    public ResponseEntity<ApiResponse<StudyMaterialMaster>> updateMaterial(@PathVariable Long id, @RequestBody StudyMaterialMaster body) {
        return ResponseEntity.ok(ApiResponse.success("Material master updated", service.saveMaterialMaster(id, body)));
    }

    @PostMapping("/material-masters/{id}/submit")
    public ResponseEntity<ApiResponse<StudyMaterialMaster>> submitMaterial(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Sent to the principal", service.submitMaterialMaster(id)));
    }

    @PostMapping("/material-masters/{id}/approve")
    public ResponseEntity<ApiResponse<StudyMaterialMaster>> approveMaterial(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Material master approved", service.approveMaterialMaster(id)));
    }

    @PostMapping("/material-masters/{id}/reject")
    public ResponseEntity<ApiResponse<StudyMaterialMaster>> rejectMaterial(@PathVariable Long id, @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.success("Material master rejected", service.rejectMaterialMaster(id, reason)));
    }

    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> upload(@RequestPart("file") MultipartFile file) throws IOException {
        return ResponseEntity.status(201).body(ApiResponse.created("File stored", service.storeFile(file)));
    }

    @GetMapping("/collections")
    public ResponseEntity<ApiResponse<Page<DocCollectionBatch>>> collections(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Document collections", service.listCollections(pageable)));
    }

    @GetMapping("/collections/{id}/items")
    public ResponseEntity<ApiResponse<List<DocCollectionItem>>> collectionItems(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Collection rows", service.collectionItems(id)));
    }

    @PostMapping("/collections")
    public ResponseEntity<ApiResponse<DocCollectionBatch>> createCollection(@RequestBody DocumentationService.CollectionBatchRequest body) {
        return ResponseEntity.status(201).body(ApiResponse.created("Collection saved", service.saveCollection(body)));
    }

    @GetMapping("/handovers")
    public ResponseEntity<ApiResponse<Page<StudyMaterialHandover>>> handovers(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Study material handovers", service.listHandovers(pageable)));
    }

    @GetMapping("/handovers/{id}/items")
    public ResponseEntity<ApiResponse<List<StudyMaterialHandoverItem>>> handoverItems(
            @PathVariable Long id, @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success("Handover rows", service.handoverItems(id, status)));
    }

    @PostMapping("/handovers")
    public ResponseEntity<ApiResponse<StudyMaterialHandover>> createHandover(@RequestBody DocumentationService.HandoverRequest body) {
        return ResponseEntity.status(201).body(ApiResponse.created("Handover saved", service.saveHandover(body)));
    }

    @PutMapping("/handovers/items/{itemId}")
    public ResponseEntity<ApiResponse<StudyMaterialHandoverItem>> updateItem(
            @PathVariable Long itemId, @RequestBody DocumentationService.HandoverItemRequest body) {
        return ResponseEntity.ok(ApiResponse.success("Handover row updated", service.updateHandoverItem(itemId, body)));
    }
}
