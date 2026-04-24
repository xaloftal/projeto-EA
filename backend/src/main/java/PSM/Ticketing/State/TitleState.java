package PSM.Ticketing.State;

public interface TitleState {

	public void activate();

	public void expire();

	public void validate();

	public String getStateName();

	public void use();
}