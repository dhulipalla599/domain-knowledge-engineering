package com.dke.data_engineering.data_quality_and_validation.integration;

import com.dke.data_engineering.data_quality_and_validation.domain.RawClaim;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Reads {@code sample-data/tpa-claims-<date>.csv} from the classpath. Simple CSV: no quoted commas. */
@Component
public class ClasspathBatchSource implements BatchSource {

    @Override
    public Optional<SourceFile> read(LocalDate businessDate) {
        String fileName = "tpa-claims-" + businessDate + ".csv";
        ClassPathResource resource = new ClassPathResource("sample-data/" + fileName);
        if (!resource.exists()) {
            return Optional.empty();
        }
        try (InputStream in = resource.getInputStream()) {
            byte[] bytes = in.readAllBytes();
            String checksum = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            List<String> lines = new String(bytes, StandardCharsets.UTF_8).lines().toList();
            List<String> header = lines.isEmpty() ? List.of()
                    : Arrays.stream(lines.get(0).split(",", -1)).map(String::trim).toList();
            List<RawClaim> rows = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.isBlank()) continue;
                String[] c = Arrays.copyOf(line.split(",", -1), 5);
                rows.add(new RawClaim(i + 1, c[0], c[1], c[2], c[3], c[4], line));
            }
            return Optional.of(new SourceFile(fileName, checksum, header, rows));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
