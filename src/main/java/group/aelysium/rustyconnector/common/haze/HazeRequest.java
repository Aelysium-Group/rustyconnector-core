package group.aelysium.rustyconnector.common.haze;

import group.aelysium.haze.exceptions.HazeException;
import group.aelysium.haze.lib.DataRequest;

public abstract class HazeRequest {
    public abstract DataRequest generate(Object ...arguments) throws HazeException;
}
