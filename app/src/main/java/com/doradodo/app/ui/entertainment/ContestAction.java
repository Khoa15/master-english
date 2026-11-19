package com.doradodo.app.ui.entertainment;

import android.content.Context;

import com.doradodo.app.R;
import com.doradodo.app.data.repository.ContestRules;
import com.doradodo.app.data.repository.EntertainmentRepository;
import com.doradodo.app.data.repository.EntertainmentRepository.ContestCard;
import com.doradodo.app.data.repository.ContestState;

/** What the main button of a contest does for this learner right now. */
final class ContestAction {

    enum Kind { REGISTER, PLAY, NONE }

    final Kind kind;
    final String label;

    private ContestAction(Kind kind, String label) {
        this.kind = kind;
        this.label = label;
    }

    static ContestAction of(Context c, ContestCard card, long now) {
        if (card.state == ContestState.ENDED) {
            return new ContestAction(Kind.NONE, c.getString(R.string.ent_c_ended));
        }
        if (card.registered) {
            return card.state == ContestState.LIVE
                    ? new ContestAction(Kind.PLAY, c.getString(R.string.ent_c_enter))
                    : new ContestAction(Kind.NONE, c.getString(R.string.ent_c_registered));
        }
        if (ContestRules.canRegister(card.contest, now)) {
            return new ContestAction(Kind.REGISTER, c.getString(R.string.ent_c_register));
        }
        return new ContestAction(Kind.NONE, card.state == ContestState.REGISTRATION_SOON
                ? c.getString(R.string.ent_c_reg_soon) : c.getString(R.string.ent_c_reg_closed));
    }

    /** "2 d", "5 h" or "15 min": the largest unit that is not zero. */
    static String remaining(Context c, long ms) {
        long minutes = Math.max(0, ms) / 60_000L;
        if (minutes >= 24 * 60) {
            return c.getString(R.string.ent_unit_d, (int) (minutes / (24 * 60)));
        }
        if (minutes >= 60) {
            return c.getString(R.string.ent_unit_h, (int) (minutes / 60));
        }
        return c.getString(R.string.ent_unit_min, (int) Math.max(1, minutes));
    }
}
