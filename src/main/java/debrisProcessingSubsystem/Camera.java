package debrisProcessingSubsystem;

import debrisProcessingSubsystem.updateSystem.*;
import fpga.memory.EmptyRegisterException;
import fpga.memory.MemoryMap;
import fpga.memory.NoSuchRegisterFoundException;
import fpga.memory.UnavailbleRegisterException;
import fpga.objectdetection.Debris;

import java.util.ArrayList;
import java.util.List;

/**
 * Team 01 will implement the Operator
 * This will be the interface implemented by the Camera object shown
 * in the SADD.
 * This is a preliminary placeholder and very subject to change.
 * Created by dsr on 3/4/17.
 */

public class Camera implements Updatable {

  List<Debris> debris = new ArrayList<>();

  public Camera() {
    this.debris = new ArrayList<>();
  }

 // Jalen
  private OperatorUpdate on() {
    try {
      MemoryMap.write("turnOnCamera", true);
    } catch (NoSuchRegisterFoundException e) {
      e.printStackTrace();
    } catch (UnavailbleRegisterException e) {
      e.printStackTrace();
    }
    return new CameraUpdate(UpdateType.COMMUNICATION_UP);
  }

  // Jalen
  private OperatorUpdate off() {
    try {
      MemoryMap.write("turnOffCamera", true);
    } catch (NoSuchRegisterFoundException e) {
      e.printStackTrace();
    } catch (UnavailbleRegisterException e) {
      e.printStackTrace();
    }
    return new CameraUpdate(UpdateType.COMMUNICATION_UP);
  }

  // Corey
  private OperatorUpdate reset() {
    return new CameraUpdate(UpdateType.COMMUNICATION_UP);
  }

  // Daniel
  private OperatorUpdate takePicture() {
    try {
      MemoryMap.write("takePicture", true);
    } catch (NoSuchRegisterFoundException e) {
      e.printStackTrace();
    } catch (UnavailbleRegisterException e) {
      e.printStackTrace();
    }
    return new CameraUpdate(UpdateType.COMMUNICATION_UP);
  }

  // Sean Hanely
  private OperatorUpdate getRawFrame() {
    return null;
  }

  // Corey
  private OperatorUpdate setZoomLevel() {
    return null;
  }

  // Divya
  private CameraUpdate process_image() {
    if (debris.isEmpty()) {
      try {
        this.debris = MemoryMap.read(debris.getClass(), "debris");
        if (debris.isEmpty()) {
          return new CameraUpdate(UpdateType.COMMUNICATION_DOWN);
        } else {
          MemoryMap.write("debris", null);
          return new CameraUpdate(UpdateType.DONE);
        }
      } catch (NoSuchRegisterFoundException e) {
        e.printStackTrace();
      } catch (EmptyRegisterException e) {
        e.printStackTrace();
      } catch (UnavailbleRegisterException e) {
        e.printStackTrace();
      }
    } else {
      return new CameraUpdate(UpdateType.DONE);
    }
    return new CameraUpdate(UpdateType.COMMUNICATION_DOWN);
  }


  public Update updateComponent(CameraUpdate theUpdate) {
    switch(theUpdate.param) {
      case TURN_ON_CAMERA:
        return on();
      case TURN_OFF_CAMERA:
        return off();
      case RESET_CAMERA:
        return reset();
      case TAKE_PICTURE:
        return takePicture();
      case PROCESS_IMAGE:
        return process_image();
      default:
        throw new RuntimeException("I don't understand what you want me to do.");
    }
  }

  public Update pollComponent() {
    if (debris.isEmpty()) {
      throw new RuntimeException("No data ready");
    } else {
      CameraUpdate camera_update = new CameraUpdate(UpdateType.DONE);
      //camera_update.setDebris(debris);
      this.debris = new ArrayList<>();
      return camera_update;
    }
  }

}
