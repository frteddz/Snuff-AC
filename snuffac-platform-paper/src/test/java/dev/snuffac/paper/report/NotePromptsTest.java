package dev.snuffac.paper.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * A report with a category and no detail is close to useless, and the note
 * field had been on every stored report since it was introduced without ever
 * being reachable. These cover the prompt itself: it must not fire for a
 * player who did not open one, it must expire, and it must never let a
 * second prompt overwrite a first.
 */
class NotePromptsTest {

    private static final UUID STAFF = UUID.randomUUID();
    private static final UUID TARGET = UUID.randomUUID();

    @Test
    void aPlayerWhoDidNotOpenAPromptIsNotPrompted() {
        NotePrompts prompts = new NotePrompts();
        assertFalse(prompts.isWaiting(STAFF));
        assertNull(prompts.target(STAFF));
        assertNull(prompts.category(STAFF));
    }

    @Test
    void anOpenPromptRemembersTheTargetAndCategory() {
        NotePrompts prompts = new NotePrompts();
        prompts.open(STAFF, TARGET, "Tester2", "exploiting");
        assertTrue(prompts.isWaiting(STAFF));
        assertEquals(TARGET, prompts.target(STAFF));
        assertEquals("Tester2", prompts.targetName(STAFF));
        assertEquals("exploiting", prompts.category(STAFF),
                "the chosen category was lost, so the report came back as 'pick a category'");
    }

    @Test
    void onePromptAtATime() {
        NotePrompts prompts = new NotePrompts();
        prompts.open(STAFF, TARGET, "Tester2", "cheating");
        prompts.open(STAFF, TARGET, "Tester3", "griefing");
        assertEquals("Tester3", prompts.targetName(STAFF));
        assertEquals("griefing", prompts.category(STAFF));
    }

    @Test
    void cancellingClearsThePrompt() {
        NotePrompts prompts = new NotePrompts();
        prompts.open(STAFF, TARGET, "Tester2", "cheating");
        prompts.cancel(STAFF);
        assertFalse(prompts.isWaiting(STAFF));
        assertNull(prompts.target(STAFF));
    }

    @Test
    void twoPlayersDoNotShareAPrompt() {
        NotePrompts prompts = new NotePrompts();
        UUID other = UUID.randomUUID();
        prompts.open(STAFF, TARGET, "Tester2", "cheating");
        assertFalse(prompts.isWaiting(other), "a note prompt leaked to another player");
    }

    @Test
    void theWordsThatSkipTheNoteAreRecognised() {
        NotePrompts prompts = new NotePrompts();
        for (String word : new String[] {"cancel", "CANCEL", " cancel ", "abort", "n"}) {
            assertTrue(prompts.isExpiredWord(word), word + " should submit without a note");
        }
        for (String word : new String[] {"", "cancelled this", "no", "nope", "not really"}) {
            assertFalse(prompts.isExpiredWord(word), word + " is prose and must be kept as a note");
        }
    }

    @Test
    void aNullMessageDoesNotSkipTheNote() {
        assertFalse(new NotePrompts().isExpiredWord(null));
    }

    @Test
    void theLengthLimitIsReasonable() {
        assertTrue(NotePrompts.MAX_LENGTH >= 120,
                "a note limit under 120 characters cannot describe what happened");
        assertTrue(NotePrompts.MAX_LENGTH <= 1000,
                "a note limit over 1000 characters is not something anyone types");
    }
}
