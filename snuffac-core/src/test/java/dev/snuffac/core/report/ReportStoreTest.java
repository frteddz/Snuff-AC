package dev.snuffac.core.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReportStoreTest {

    private static final UUID REPORTER = UUID.randomUUID();
    private static final UUID TARGET = UUID.randomUUID();

    @Test
    void thereAreExactlySevenCategories() {
        assertEquals(7, ReportCategory.ALL.size());
        assertEquals(7, ReportCategory.all().size());
    }

    @Test
    void everyCategoryHasALabelAndADescription() {
        for (ReportCategory.Definition definition : ReportCategory.all()) {
            assertNotNull(definition.label());
            assertFalse(definition.label().isBlank());
            assertFalse(definition.description().isBlank());
        }
    }

    @Test
    void theThreeCategoriesTheUserNamedArePresent() {
        assertNotNull(ReportCategory.get("cheating"));
        assertNotNull(ReportCategory.get("language"));
        assertNotNull(ReportCategory.get("offensive"));
    }

    @Test
    void categoryLookupIsCaseInsensitive() {
        assertNotNull(ReportCategory.get("CHEATING"));
        assertNotNull(ReportCategory.get(" Cheating "));
        assertTrue(ReportCategory.isValid("griefing"));
        assertFalse(ReportCategory.isValid("banana"));
        assertFalse(ReportCategory.isValid(null));
    }

    @Test
    void aNoteCannotSmuggleMarkupOrNewlines() {
        String cleaned = ReportCategory.sanitiseNote("hello & <red> world\nsecond", 280);
        assertFalse(cleaned.contains("&"));
        assertFalse(cleaned.contains("<"));
        assertFalse(cleaned.contains("\n"));
        assertTrue(cleaned.contains("hello"));
        assertTrue(cleaned.contains("world"));
    }

    @Test
    void aNoteIsLengthLimited() {
        String cleaned = ReportCategory.sanitiseNote("x".repeat(5000), 280);
        assertEquals(280, cleaned.length());
    }

    @Test
    void aNullNoteBecomesEmpty() {
        assertEquals("", ReportCategory.sanitiseNote(null, 280));
    }

    @Test
    void filingAReportStoresItOpen() {
        ReportStore store = new ReportStore(null);
        var entry = store.file(REPORTER, "reporter", TARGET, "target",
                ReportCategory.CHEATING, "they were flying");
        assertNotNull(entry);
        assertTrue(entry.open());
        assertEquals(ReportCategory.CHEATING, entry.category());
        assertEquals("they were flying", entry.note());
        assertEquals(1, store.filedCount());
        assertEquals(1, store.openCount());
    }

    @Test
    void anUnknownCategoryIsRefused() {
        ReportStore store = new ReportStore(null);
        assertNull(store.file(REPORTER, "r", TARGET, "t", "nonsense", "note"));
        assertEquals(0, store.filedCount());
    }

    @Test
    void aReporterIsRateLimited() {
        ReportStore store = new ReportStore(null);
        for (int i = 0; i < 5; i++) {
            store.file(REPORTER, "r", TARGET, "t", ReportCategory.CHEATING, "n");
        }
        assertTrue(store.rateLimited(REPORTER), "five reports inside the window must trip the limit");
    }

    @Test
    void oneReporterBeingLimitedDoesNotLimitAnother() {
        ReportStore store = new ReportStore(null);
        for (int i = 0; i < 5; i++) {
            store.file(REPORTER, "r", TARGET, "t", ReportCategory.CHEATING, "n");
        }
        assertFalse(store.rateLimited(UUID.randomUUID()));
    }

    @Test
    void claimingAndResolvingMovesTheReportOutOfOpen() {
        ReportStore store = new ReportStore(null);
        var entry = store.file(REPORTER, "r", TARGET, "t", ReportCategory.OFFENSIVE, "n");
        UUID staff = UUID.randomUUID();
        entry.claim(staff, "mod");
        assertTrue(entry.claimed());
        assertEquals(staff, entry.handledBy());
        assertEquals("mod", entry.handledByName());
        assertEquals(0, store.openCount());

        entry.resolve("warned");
        assertEquals("RESOLVED", entry.status());
        assertEquals("warned", entry.resolution());
    }

    @Test
    void releasingAClaimMakesItOpenAgain() {
        ReportStore store = new ReportStore(null);
        var entry = store.file(REPORTER, "r", TARGET, "t", ReportCategory.GRIEFING, "n");
        entry.claim(UUID.randomUUID(), "mod");
        entry.release();
        assertTrue(entry.open());
        assertNull(entry.handledBy());
        assertEquals(1, store.openCount());
    }

    @Test
    void reportsComeBackNewestFirst() {
        ReportStore store = new ReportStore(null);
        store.file(REPORTER, "r", TARGET, "t", ReportCategory.CHEATING, "first");
        store.file(REPORTER, "r", TARGET, "t", ReportCategory.NAME, "second");
        List<ReportCategory.Entry> all = store.all();
        assertEquals("second", all.get(0).note());
        assertEquals("first", all.get(1).note());
    }

    @Test
    void byTargetOnlyReturnsThatPlayersReports() {
        ReportStore store = new ReportStore(null);
        UUID other = UUID.randomUUID();
        store.file(REPORTER, "r", TARGET, "t", ReportCategory.CHEATING, "mine");
        store.file(REPORTER, "r", other, "o", ReportCategory.CHEATING, "theirs");
        assertEquals(1, store.byTarget(TARGET).size());
        assertEquals("mine", store.byTarget(TARGET).get(0).note());
    }

    @Test
    void filteringNarrowsByStatusAndCategory() {
        ReportStore store = new ReportStore(null);
        var open = store.file(REPORTER, "r", TARGET, "t", ReportCategory.CHEATING, "open one");
        store.file(REPORTER, "r", TARGET, "t", ReportCategory.NAME, "closed one");
        store.byId(store.all().get(0).id()).resolve("handled");
        assertEquals(1, ReportCategory.filter(store.all(), "OPEN", "").size());
        assertEquals(2, ReportCategory.filter(store.all(), "", "").size());
        assertEquals(1, ReportCategory.filter(store.all(), "", ReportCategory.NAME).size());
        assertNotNull(open);
    }

    @Test
    void retentionDropsOldReports() {
        ReportStore store = new ReportStore(null);
        store.retentionDays(0L);
        var entry = store.file(REPORTER, "r", TARGET, "t", ReportCategory.CHEATING, "n");
        assertEquals(1, store.prune(System.currentTimeMillis() + 60_000L));
        assertEquals(0, store.filedCount());
        assertNull(store.byId(entry.id()));
    }

    @Test
    void aReportSurvivesASaveAndReload(@TempDir Path dir) {
        Path file = dir.resolve("reports.tsv");
        ReportStore store = new ReportStore(file);
        var entry = store.file(REPORTER, "reporter", TARGET, "target",
                ReportCategory.IMPERSONATION, "they posed as an admin");
        entry.claim(UUID.randomUUID(), "mod");
        store.save();

        ReportStore reloaded = new ReportStore(file);
        reloaded.load();
        assertEquals(1, reloaded.filedCount());
        var back = reloaded.byId(entry.id());
        assertNotNull(back);
        assertEquals(TARGET, back.target());
        assertEquals(REPORTER, back.reporter());
        assertEquals("they posed as an admin", back.note());
        assertTrue(back.claimed());
        assertEquals("mod", back.handledByName());
    }

    @Test
    void loadingAMissingFileIsSafe(@TempDir Path dir) {
        ReportStore store = new ReportStore(dir.resolve("nope.tsv"));
        store.load();
        assertTrue(store.loaded());
        assertEquals(0, store.filedCount());
    }

    @Test
    void encodingRoundTripsAwkwardCharacters() {
        var map = new java.util.LinkedHashMap<String, Object>();
        map.put("note", "a\tb\nc");
        map.put("category", ReportCategory.CHEATING);
        String encoded = ReportStore.encode(map);
        var decoded = ReportStore.decode(encoded);
        assertEquals("a\tb\nc", decoded.get("note"));
        assertEquals(ReportCategory.CHEATING, decoded.get("category"));
    }
}
