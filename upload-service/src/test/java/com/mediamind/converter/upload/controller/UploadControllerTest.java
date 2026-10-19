package com.mediamind.converter.upload.controller;

import com.mediamind.converter.upload.exception.UploadFailureException;
import com.mediamind.converter.upload.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UploadController.class)
@AutoConfigureMockMvc
public class UploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("removal")
    @MockBean
    private FileStorageService storageService;

    @Test
    void testUploadControllerHandlesUploadFailureException() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "video.mp4", "video/mp4", new byte[1024]);

        when(storageService.store(file, "matroska")).thenThrow(new UploadFailureException("video.mp4", "Upload failed"));

        MvcResult result = mockMvc.perform(
                multipart("/upload")
                        .file(file)
                        .param("outputFormat", "matroska"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("video.mp4"));
        assertTrue(response.contains("Upload failed"));
    }
}
