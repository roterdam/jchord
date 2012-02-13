package chord.analyses.typestate;
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import joeq.Compiler.Quad.RegisterFactory.Register;

/**
 *
 * @author machiry
 */
public class TypeStateMap {
    
    List<Register> pointsToRegisters;
    String currentState;
    StateMap stateMap;
    String errorStateName ="error";
    String initialStateName = "start";
    List<String> methodTrace;

    public TypeStateMap(StateMap stateMap){
        this.stateMap = stateMap;
        this.pointsToRegisters = new LinkedList<Register>();
        this.currentState = initialStateName;
        this.methodTrace = new LinkedList<String>();
    }

    public Boolean IsInPointsToRegister(Register r){
        return this.pointsToRegisters.contains(r);
    }

    public Boolean IsTheObjectUnTrackable(){
        return this.pointsToRegisters.isEmpty();
    }

    public void AddPointsToRegister(Register r){
        if(!this.pointsToRegisters.contains(r)){
            this.pointsToRegisters.add(r);
        }
    }

    public void RemovePointsToRegister(Register r){
        if(this.pointsToRegisters.contains(r)){
            this.pointsToRegisters.remove(r);
        }
    }

    public String GetCurrentState(){
        return this.currentState;
    }

    public List<String> GetMethodTrace(){
        return this.methodTrace;
    }

    public Boolean TryTransition(String methodName){
        Boolean isSucess = true;
        this.methodTrace.add(methodName);
        if(!this.currentState.toLowerCase().equals(errorStateName)){
            String methodTargetState = this.stateMap.GetMethodTransition(methodName,this.currentState);
            isSucess = false;
            if(methodTargetState != null){
                if(this.stateMap.IsValidTransition(currentState, methodTargetState)){
                    isSucess = true;
                    this.currentState = methodTargetState;
                }   else{
                    this.currentState = errorStateName;
                }
            }
        }

        return isSucess;
    }

}

