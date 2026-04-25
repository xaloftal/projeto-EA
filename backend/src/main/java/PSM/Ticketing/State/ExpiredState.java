package PSM.Ticketing.State;

public class ExpiredState implements TitleState {

	public void validate() {
		throw new UnsupportedOperationException();
	}

	public void activate() {
		throw new UnsupportedOperationException();
	}

	public void expire() {
		throw new UnsupportedOperationException();
	}

	public void use() {
		throw new UnsupportedOperationException();
	}

	public String getStateName() {
		throw new UnsupportedOperationException();
	}
}