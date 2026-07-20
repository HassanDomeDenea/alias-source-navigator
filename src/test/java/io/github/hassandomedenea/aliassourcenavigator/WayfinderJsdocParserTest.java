package io.github.hassandomedenea.aliassourcenavigator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

final class WayfinderJsdocParserTest {
    private static final String REAL_DOC = """
        /**
         * @see \\App\\Modules\\Patients\\PatientController::autocomplete
         * @see \\E:\\Herd\\hddclinic4\\app\\Modules\\Patients\\PatientController.php:52
         * @route "/api/patients/autocomplete"
         */
        """;

    @Test
    void parsesFqnAndWindowsPathFromWayfinderDoc() {
        WayfinderJsdocParser.PhpTarget target = WayfinderJsdocParser.parse(REAL_DOC);

        assertNotNull(target);
        assertEquals("\\App\\Modules\\Patients\\PatientController", target.classFqn());
        assertEquals("autocomplete", target.methodName());
        assertEquals("E:\\Herd\\hddclinic4\\app\\Modules\\Patients\\PatientController.php", target.filePath());
        assertEquals(52, target.line());
    }

    @Test
    void parsesUnixPathFallback() {
        WayfinderJsdocParser.PhpTarget target = WayfinderJsdocParser.parse("""
            /**
             * @see /home/user/app/Http/Controllers/UserController.php:18
             */
            """);

        assertNotNull(target);
        assertEquals("/home/user/app/Http/Controllers/UserController.php", target.filePath());
        assertEquals(18, target.line());
    }

    @Test
    void returnsNullForUnrelatedComments() {
        assertNull(WayfinderJsdocParser.parse("/** just a comment */"));
    }

    @Test
    void findsDocCommentImmediatelyBeforeExport() {
        String fileText = """
            // header

            /**
             * @see \\App\\Modules\\Patients\\PatientController::autocomplete
             * @see \\E:\\Herd\\hddclinic4\\app\\Modules\\Patients\\PatientController.php:52
             * @route "/api/patients/autocomplete"
             */
            export const autocomplete = () => ({})
            """;

        int exportOffset = fileText.indexOf("export const autocomplete");
        String doc = WayfinderJsdocParser.findDocCommentBefore(fileText, exportOffset);

        assertNotNull(doc);
        WayfinderJsdocParser.PhpTarget target = WayfinderJsdocParser.parse(doc);
        assertNotNull(target);
        assertEquals("autocomplete", target.methodName());
    }

    @Test
    void findsDocCommentWhenOffsetIsTheExportedIdentifier() {
        // Runtime uses the name offset from namedExportOffset(), not the "export" keyword.
        String fileText = """
            /**
             * @see \\App\\Modules\\Patients\\PatientController::autocomplete
             * @see \\E:\\Herd\\hddclinic4\\app\\Modules\\Patients\\PatientController.php:52
             * @route "/api/patients/autocomplete"
             */
            export const autocomplete = () => ({})
            """;

        Integer nameOffset = WayfinderTsExportResolver.namedExportOffset(fileText, "autocomplete");
        assertNotNull(nameOffset);

        String doc = WayfinderJsdocParser.findDocCommentBefore(fileText, nameOffset);
        assertNotNull(doc);

        WayfinderJsdocParser.PhpTarget target = WayfinderJsdocParser.parse(doc);
        assertNotNull(target);
        assertEquals("\\App\\Modules\\Patients\\PatientController", target.classFqn());
        assertEquals("autocomplete", target.methodName());
        assertEquals("E:\\Herd\\hddclinic4\\app\\Modules\\Patients\\PatientController.php", target.filePath());
        assertEquals(52, target.line());
    }

    @Test
    void ignoresDocCommentWhenOtherCodeIsBetween() {
        String fileText = """
            /**
             * @see \\App\\Modules\\Patients\\PatientController::query
             */
            const unrelated = 1
            export const autocomplete = () => ({})
            """;

        int exportOffset = fileText.indexOf("export const autocomplete");
        assertNull(WayfinderJsdocParser.findDocCommentBefore(fileText, exportOffset));

        Integer nameOffset = WayfinderTsExportResolver.namedExportOffset(fileText, "autocomplete");
        assertNotNull(nameOffset);
        assertNull(WayfinderJsdocParser.findDocCommentBefore(fileText, nameOffset));
    }
}
