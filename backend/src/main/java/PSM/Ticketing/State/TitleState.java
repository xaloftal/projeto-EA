package PSM.Ticketing.State;

import PSM.Ticketing.Title;

public interface TitleState {

	default void activate(Title title) { throw new UnsupportedOperationException(); }

	default void expire(Title title) { throw new UnsupportedOperationException(); }

	default void validate(Title title) { throw new UnsupportedOperationException(); }

	public String getStateName();

	default void use(Title title) { throw new UnsupportedOperationException(); }
}