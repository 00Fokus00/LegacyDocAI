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

import java.util.Arrays;
import java.util.List;

@Service
public class DocumentLoaderService {

    private final DocumentRepository documentRepository;

    private final ResourcePatternResolver resolver;

    private final VectorStore vectorStore;

    public DocumentLoaderService(DocumentRepository documentRepository, ResourcePatternResolver resolver, VectorStore vectorStore) {
        this.documentRepository = documentRepository;
        this.resolver = resolver;
        this.vectorStore = vectorStore;
    }


    @SneakyThrows
    public void loadDocuments(String projectPath) {

        String normalizedPath = projectPath.replace("\\", "/");

        if (!normalizedPath.endsWith("/")) {
            normalizedPath += "/";
        }

        String locationPattern = "file:" + normalizedPath + "**/*.java";

        List<Resource> resources = Arrays.stream(resolver.getResources(locationPattern)).toList();
        List<LoadedDocument> allLoadedDocs = documentRepository.findAll();

        for (LoadedDocument doc : allLoadedDocs) {
            String filename = doc.getFilename();

            Resource matchingResource = resources.stream()
                    .filter(r -> filename.equals(r.getFilename()))
                    .findFirst()
                    .orElse(null);

            String currentHash = matchingResource != null ? calcContentHash(matchingResource) : null;

            // проводим зачистку старых данных
            if (currentHash == null || !currentHash.equals(doc.getContentHash())) {
                documentRepository.deleteVectorsByFilename(filename);
                documentRepository.deleteByFilename(filename);
            }
        }

        resources.stream()
                .map(resource -> Pair.of(resource, calcContentHash(resource)))
                .filter(pair -> !documentRepository.existsByFilenameAndContentHash(pair.getFirst().getFilename(), pair.getSecond()))
                .forEach(pair -> {
                    Resource resource = pair.getFirst();
                    List<Document> documents = new TextReader(resource).get();
                    TokenTextSplitter textSplitter = TokenTextSplitter.builder().withChunkSize(500).build();
                    List<Document> chunks = textSplitter.apply(documents);
                    vectorStore.accept(chunks);

                    LoadedDocument loadedDocument = LoadedDocument.builder()
                            .documentType("java")
                            .chunkCount(chunks.size())
                            .filename(resource.getFilename())
                            .contentHash(pair.getSecond())
                            .build();
                    documentRepository.save(loadedDocument);

                });


    }

    @SneakyThrows
    private String calcContentHash(Resource resource) {
        return DigestUtils.md5DigestAsHex(resource.getInputStream());
    }
}

