package PSM.Ticketing.State;

import PSM.Ticketing.Title;

public class ValidatedState implements TitleState {

	public void use(Title title) { title.setStatus(new UsedState()); }

	public String getStateName() { return "USED"; }
}