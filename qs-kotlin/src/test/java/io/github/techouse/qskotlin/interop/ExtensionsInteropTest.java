package io.github.techouse.qskotlin.interop;

import io.github.techouse.qskotlin.QS;
import io.github.techouse.qskotlin.enums.Format;
import io.github.techouse.qskotlin.enums.ListFormat;
import io.github.techouse.qskotlin.models.EncodeOptions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static io.github.techouse.qskotlin.fixtures.data.E2EFixtures.EndToEndTestCases;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class ExtensionsInteropTest {

    @Test
    void string_toQueryMap_decodes_all_fixtures() {
        for (var tc : EndToEndTestCases) {
            Map<String, Object> expected = (Map<String, Object>) tc.getData();

            Map<String, Object> actual = QS.toQueryMap(tc.getEncoded());

            assertEquals(expected, actual, "decode mismatch for: " + tc.getEncoded());
        }
    }

    @Test
    void map_toQueryString_encodes_all_fixtures() {
        for (var tc : EndToEndTestCases) {
            Map<String, Object> input = (Map<String, Object>) tc.getData();

            EncodeOptions opts = new EncodeOptions(null, null, ListFormat.INDICES, null, null, false, false, StandardCharsets.UTF_8, false, "&", false, false, true, Format.RFC3986, null, false, false, null, null);

            String qs = QS.toQueryString(input, opts);
            assertEquals(tc.getEncoded(), qs, "encode mismatch for: " + input);
        }
    }
}
