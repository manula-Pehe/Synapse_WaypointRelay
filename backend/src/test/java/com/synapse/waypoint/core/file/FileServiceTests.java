package com.synapse.waypoint.core.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.common.error.NotFoundException;
import com.synapse.waypoint.core.file.dto.FileContent;
import com.synapse.waypoint.core.file.entity.FileKind;
import com.synapse.waypoint.core.file.service.FileService;

/** Stores invented bytes through {@link FileService}. Each test rolls back. */
@SpringBootTest
@Transactional
class FileServiceTests {

    private static final byte[] PHOTO_BYTES = {1, 2, 3, 4, 5};
    private static final int TEN_MB = 10 * 1024 * 1024;

    @Autowired FileService files;
    @Autowired JdbcTemplate jdbc;

    @Test
    void shouldReturnTheStoredBytesAndContentType() {
        String id = files.store(PHOTO_BYTES, "image/jpeg", FileKind.PHOTO, null);

        FileContent file = files.get(id);

        assertThat(file.id()).isEqualTo(id);
        assertThat(file.contentType()).isEqualTo("image/jpeg");
        assertThat(file.bytes()).isEqualTo(PHOTO_BYTES);
    }

    @Test
    void shouldGiveEveryFileAnId() {
        String first = files.store(PHOTO_BYTES, "image/png", FileKind.SIGNATURE, null);
        String second = files.store(PHOTO_BYTES, "image/png", FileKind.SIGNATURE, null);

        assertThat(first).startsWith("f-").isNotEqualTo(second);
    }

    @Test
    void shouldAcceptWebpAndAFileOfExactlyTenMegabytes() {
        assertThat(files.store(PHOTO_BYTES, "image/webp", FileKind.PHOTO, null)).isNotBlank();
        assertThat(files.store(new byte[TEN_MB], "image/jpeg", FileKind.PHOTO, null)).isNotBlank();
    }

    @Test
    void shouldReturnTheExistingIdWhenTheSameClientIdIsStoredTwice() {
        String first = files.store(PHOTO_BYTES, "image/jpeg", FileKind.PHOTO, "client-retry-1");
        String retry = files.store(new byte[] {9, 9}, "image/jpeg", FileKind.PHOTO, "client-retry-1");

        assertThat(retry).isEqualTo(first);
        assertThat(files.get(first).bytes()).isEqualTo(PHOTO_BYTES);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM files WHERE client_id = 'client-retry-1'",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void shouldKeepDifferentClientIdsApart() {
        String first = files.store(PHOTO_BYTES, "image/jpeg", FileKind.PHOTO, "client-a");
        String second = files.store(PHOTO_BYTES, "image/jpeg", FileKind.PHOTO, "client-b");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void shouldLeaveUploadedByEmptyWhenNobodyIsSignedIn() {
        String id = files.store(PHOTO_BYTES, "image/jpeg", FileKind.PHOTO, null);

        assertThat(jdbc.queryForObject("SELECT uploaded_by FROM files WHERE id = ?", String.class, id)).isNull();
    }

    @Test
    void shouldRejectATypeThatIsNotAnAllowedImage() {
        assertValidationError(() -> files.store(PHOTO_BYTES, "application/pdf", FileKind.PHOTO, null));
        assertValidationError(() -> files.store(PHOTO_BYTES, "image/gif", FileKind.PHOTO, null));
        assertValidationError(() -> files.store(PHOTO_BYTES, null, FileKind.PHOTO, null));
    }

    @Test
    void shouldRejectAFileOverTenMegabytes() {
        assertValidationError(() -> files.store(new byte[TEN_MB + 1], "image/jpeg", FileKind.PHOTO, null));
    }

    @Test
    void shouldRejectAnEmptyFile() {
        assertValidationError(() -> files.store(new byte[0], "image/jpeg", FileKind.PHOTO, null));
    }

    @Test
    void shouldThrowNotFoundForAnUnknownId() {
        assertThatThrownBy(() -> files.get("f-does-not-exist")).isInstanceOf(NotFoundException.class);
    }

    private static void assertValidationError(Runnable call) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(DomainException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION));
    }
}
