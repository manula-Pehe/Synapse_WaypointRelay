package com.synapse.waypoint.core.file.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.error.NotFoundException;
import com.synapse.waypoint.common.security.CurrentUser;
import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.core.file.dto.FileContent;
import com.synapse.waypoint.core.file.entity.FileKind;
import com.synapse.waypoint.core.file.entity.StoredFile;
import com.synapse.waypoint.core.file.repository.StoredFileRepository;

@Service
@Transactional
class DefaultFileService implements FileService {

    private static final String RESOURCE = "File";
    private static final String ID_PREFIX = "f-";

    private final StoredFileRepository files;
    private final FileUploadPolicy policy;
    private final CurrentUser currentUser;
    private final DemoClock clock;

    DefaultFileService(StoredFileRepository files, FileUploadPolicy policy, CurrentUser currentUser,
            DemoClock clock) {
        this.files = files;
        this.policy = policy;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Override
    public String store(byte[] bytes, String contentType, FileKind kind, String clientId) {
        policy.check(bytes, contentType);
        String id = ID_PREFIX + UUID.randomUUID();
        int written = files.insertIfClientIdFree(id, kind.name(), contentType, bytes.length, bytes,
                clientId, currentUser.idIfSignedIn().orElse(null), clock.now());
        return written == 1 ? id : existingIdFor(clientId);
    }

    @Override
    @Transactional(readOnly = true)
    public FileContent get(String fileId) {
        StoredFile file = files.findById(fileId).orElseThrow(() -> new NotFoundException(RESOURCE, fileId));
        return new FileContent(file.getId(), file.getContentType(), file.getData());
    }

    private String existingIdFor(String clientId) {
        return files.findIdByClientId(clientId).orElseThrow(() -> new NotFoundException(RESOURCE, clientId));
    }
}
