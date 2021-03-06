package debrisProcessingSubsystem.updateSystem;

/**
 * This is the parent class for all updates.
 * Created by jdt on 3/10/17.
 */
public abstract class Update {

    private final UpdateType updateType;
    
    public Update(final UpdateType updateType)
    {
      this.updateType = updateType;
    }
    
    public UpdateType getUpdateType()
    {
      return this.updateType;
    }
}
