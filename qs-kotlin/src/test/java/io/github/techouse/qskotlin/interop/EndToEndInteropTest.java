package io.github.techouse.qskotlin.interop;

import io.github.techouse.qskotlin.QS;
import io.github.techouse.qskotlin.enums.Format;
import io.github.techouse.qskotlin.enums.ListFormat;
import io.github.techouse.qskotlin.models.EncodeOptions;
import io.github.techouse.qskotlin.models.StringDelimiter;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static io.github.techouse.qskotlin.fixtures.data.E2EFixtures.EndToEndTestCases;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class EndToEndInteropTest {
    @Test
    void allCasesEncodeAndDecodeCorrectly() {
        for (var tc : EndToEndTestCases) {
            EncodeOptions opts = new EncodeOptions(null, null, ListFormat.INDICES, null, null, false, false, StandardCharsets.UTF_8, false, new StringDelimiter("&"), false, false, true, Format.RFC3986, null, false, false, null, null);

            String encoded = QS.encode((Map<String, Object>) tc.getData(), opts);
            assertEquals(tc.getEncoded(), encoded, "encode mismatch for data=" + tc.getData());

            // Decode back and compare deep equality
            Map<String, Object> decoded = QS.decode(tc.getEncoded());
            assertEquals(tc.getData(), decoded, "decode mismatch for encoded=" + tc.getEncoded());
        }
    }
}
