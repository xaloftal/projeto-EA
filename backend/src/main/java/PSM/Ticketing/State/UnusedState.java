package PSM.Ticketing.State;

import PSM.Ticketing.Title;

public class UnusedState implements TitleState {

	public void validate(Title title) { title.setStatus(new ValidatedState()); }

	public void expire(Title title) { title.setStatus(new ExpiredState()); }

	public String getStateName() { return "UNUSED"; }
}