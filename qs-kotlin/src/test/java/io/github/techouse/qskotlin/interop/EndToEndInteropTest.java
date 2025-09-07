package io.github.techouse.qskotlin.interop;

import io.github.techouse.qskotlin.QS;
import io.github.techouse.qskotlin.enums.ListFormat;
import io.github.techouse.qskotlin.models.EncodeOptions;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.github.techouse.qskotlin.fixtures.data.E2EFixtures.EndToEndTestCases;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class EndToEndInteropTest {
    @Test
    void allCasesEncodeAndDecodeCorrectly() {
        for (var tc : EndToEndTestCases) {
            EncodeOptions opts = EncodeOptions.builder()
                    .listFormat(ListFormat.INDICES)
                    .encode(false)            // mirror Kotlin: EncodeOptions(encode = false)
                    .delimiter("&")          // deterministic delimiter
                    .build();

            String encoded = QS.encode(tc.getData(), opts);
            assertEquals(tc.getEncoded(), encoded, "encode mismatch for data=" + tc.getData());

            // Decode back and compare deep equality
            Map<String, Object> decoded = QS.decode(tc.getEncoded());
            assertEquals(tc.getData(), decoded, "decode mismatch for encoded=" + tc.getEncoded());
        }
    }
}
