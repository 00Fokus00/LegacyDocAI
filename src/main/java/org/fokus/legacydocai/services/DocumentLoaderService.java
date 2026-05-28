package org.fokus.legacydocai.services;

import lombok.SneakyThrows;
import org.fokus.legacydocai.model.LoadedDocument;
import org.fokus.legacydocai.repository.DocumentRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class DocumentLoaderService {

    private final DocumentRepository documentRepository;

    private final ResourcePatternResolver resolver;

    private final VectorStore vectorStore;

    private final Map<String, String> extensionToLanguageMap;

    public DocumentLoaderService(DocumentRepository documentRepository, ResourcePatternResolver resolver, VectorStore vectorStore, Map<String, String> extensionToLanguageMap) {
        this.documentRepository = documentRepository;
        this.resolver = resolver;
        this.vectorStore = vectorStore;
        this.extensionToLanguageMap = extensionToLanguageMap;
    }


    @SneakyThrows
    public void loadDocuments(String projectPath) {

        String normalizedPath = projectPath.replace("\\", "/");

        if (!normalizedPath.endsWith("/")) {
            normalizedPath += "/";
        }

        List<Resource> resources = new ArrayList<>();
        for (String extension : extensionToLanguageMap.keySet()) {
            String locationPattern = "file:" + normalizedPath + "**/*." + extension;
            Resource[] foundResources = resolver.getResources(locationPattern);
            resources.addAll(Arrays.asList(foundResources));
        }

        cleanOldOrChangedDocuments(resources);

        resources.stream()
                .map(resource -> Pair.of(resource, calcContentHash(resource)))
                .filter(pair -> !documentRepository.existsByFilenameAndContentHash(pair.getFirst().getFilename(), pair.getSecond()))
                .forEach(pair -> {
                    Resource resource = pair.getFirst();
                    String filename = resource.getFilename();
                    String extension = StringUtils.getFilenameExtension(filename);
                    String docType = extensionToLanguageMap.getOrDefault(extension, "unknown");

                    List<Document> documents = new TextReader(resource).get();
                    TokenTextSplitter textSplitter = TokenTextSplitter.builder().withChunkSize(250).build();
                    List<Document> chunks = textSplitter.apply(documents);
                    vectorStore.accept(chunks);

                    LoadedDocument loadedDocument = LoadedDocument.builder()
                            .documentType(docType)
                            .chunkCount(chunks.size())
                            .filename(resource.getFilename())
                            .contentHash(pair.getSecond())
                            .build();
                    documentRepository.save(loadedDocument);

                });
    }

    /**
     * Метод для очистки старых данных в БД и векторном хранилище,
     * если файл был удален или его хэш изменился.
     */
    private void cleanOldOrChangedDocuments(List<Resource> currentResources) {

        List<LoadedDocument> allLoadedDocs = documentRepository.findAll();

        for (LoadedDocument doc : allLoadedDocs) {
            String filename = doc.getFilename();

            Resource matchingResource = currentResources.stream()
                    .filter(r -> filename.equals(r.getFilename()))
                    .findFirst()
                    .orElse(null);

            String currentHash = matchingResource != null ? calcContentHash(matchingResource) : null;

            // Если файл удален с диска или хэш перестал совпадать — удаляем из БД и VectorStore
            if (currentHash == null || !currentHash.equals(doc.getContentHash())) {
                documentRepository.deleteVectorsByFilename(filename);
                documentRepository.deleteByFilename(filename);
            }
        }
    }

    @SneakyThrows
    private String calcContentHash(Resource resource) {
        return DigestUtils.md5DigestAsHex(resource.getInputStream());
    }
}

