/**
 *
@see <a href="http://chord.stanford.edu/javadoc/">ChordDoc</a>
 */

import chord.project.Chord;
import chord.project.ClassicProject;
import chord.util.tuple.object.Pair;
import java.util.LinkedList;
import java.util.List;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operand.FieldOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;
import joeq.Compiler.Quad.Operator;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import joeq.Class.jq_LocalVarTableEntry;
import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator;
import joeq.UTF.Utf8;
import chord.analyses.alias.CIPAAnalysis;
import chord.analyses.alias.CIObj;
import java.io.*;

@Chord(name = "typeState")
public class TypeStateAnalysis extends DataflowAnalysis<Pair<Quad, Register>> {

    int noOfLoopTries;
    Boolean isProgramValid;
    CIPAAnalysis cipa;
    
    BufferedWriter out;
    BufferedWriter out2;

    @Override
    public void doAnalysis() {

        StateMap stateSpec = null;
        this.noOfLoopTries = 2;
        this.isProgramValid = true;
        if ((stateSpec = TryParseStateSpecFile()) == null) {
            System.out.println("Problem Parsing State Spec File. Check if the file is present and formatted correctly");
		
            return;
        }
        try
        {
        FileWriter wri = new FileWriter("out.txt");
        out = new BufferedWriter(wri);
        out2 = new BufferedWriter(new FileWriter("points2.txt"));
        }
        catch (Exception e) {
			// TODO: handle exception
		}
        
        
        ControlFlowGraph mainCfg = main.getCFG();
        
        cipa = (CIPAAnalysis)ClassicProject.g().getTrgt("cipa-java");
        ClassicProject.g().runTask(cipa);
        
        
        //int index = 0;

        //This is a map to maintain the method summay for each method(this is indexed by method signature)
        Map<String, MethodSummary> methodSummaryMap = new HashMap<String, MethodSummary>();
        //All registers which are of interest to us
        List<Register> pointsOfInterest = new LinkedList<Register>();
        //State of all Heap Allocated objects and their points to registers
        List<TypeStateMap> typeStateList = new LinkedList<TypeStateMap>();

        //1.First get the Basic Block List in DFS order
        List<joeq.Compiler.Quad.BasicBlock> basicBlocksInMain = GetBBInDFSOrder(mainCfg, this.noOfLoopTries);

        //2.Try to traverse the Basic Blocks block by block
        for (int j = 0; j < basicBlocksInMain.size(); j++) {
            joeq.Compiler.Quad.BasicBlock currentBasicBlock = basicBlocksInMain.get(j);
            ////System.out.println("Processing BasicBlock:" + currentBasicBlock.toString());
            //System.out.println(currentBasicBlock.fullDump());
            ////System.out.println("Points of Interest Before Processing Block:" + pointsOfInterest.size());
            ProcessBasicBlock(pointsOfInterest, typeStateList, stateSpec, currentBasicBlock, methodSummaryMap);
            ////System.out.println("Points of Interest After Processing Block:" + pointsOfInterest.size());
            ////System.out.flush();
        }

        if (!this.isProgramValid) {
            System.out.flush();
            System.out.println();
            System.out.println("The Program is INVALID. The State Changes might lead to Error State");
            System.out.println("Refer the log above for more details");
            System.out.println();
        } else {
            System.out.flush();
            System.out.println();
            System.out.println("The Program is VALID");
            System.out.println();
        }
        try {
			out.close();
			out2.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    }

    private MethodSummary GetMethodSummary(Quad currentInvokedQuad, StateMap stateMap, List<String> callList, Map<String, MethodSummary> summaryMap) {

        MethodSummary summary = new MethodSummary(Operator.Invoke.getMethod(currentInvokedQuad).toString());
        //System.out.println("Started to compute Message summary for:" + summary.GetMethodSignature());
        ControlFlowGraph cfgOfMethod = fullProgram.getMethod(summary.GetMethodSignature()).getCFG();

        //Set up the input parameters and output return value
        //ASSUMPTION:
        //input parameters will be of the form
        //R0,R1,R2...
        
        
        ParamListOperand parameters = Operator.Invoke.getParamList(currentInvokedQuad);
        for (int i = 0; i < parameters.length(); i++) {
            summary.AddParam("R" + i);
        }

        //Check to see if the return type is same as type we want to track
        //Care for Return variable only if this is of interesting type
        jq_Type returnType = fullProgram.getMethod(summary.GetMethodSignature()).getReturnType();

        if (returnType.getName().equals(stateMap.GetConstructorMethod())) {
            ////System.out.println("Return type of method:"+summary.GetMethodSignature()+ " is interesting");
            summary.SetReturnTypeInteresting();
        } else {
            ////System.out.println("Return type of method:"+summary.GetMethodSignature()+ " is NOT interesting");
            summary.SetReturnTypeNotInteresting();
        }

        //Track only those input paramters which are of interesting type (i.e type we need to track)
        jq_Type[] paramTypes = fullProgram.getMethod(summary.GetMethodSignature()).getParamTypes();
        List<String> trackableInputRegisteries = new LinkedList<String>();
        for (int i = 0; i < paramTypes.length; i++) {
            if (paramTypes[i].getName().equals(stateMap.GetConstructorMethod())) {
                trackableInputRegisteries.add("R" + i);
                ////System.out.println("Added R"+i+ " to the registers to be tracked");
            }
        }


        Map<String, TypeStateMap> inputTransformations = new HashMap<String, TypeStateMap>();

        List<TypeStateMap> typeState = new LinkedList<TypeStateMap>();

        List<joeq.Compiler.Quad.BasicBlock> bbList = GetBBInDFSOrder(cfgOfMethod, this.noOfLoopTries);

        List<Register> workingRegSet = new LinkedList<Register>();
        for (int i = 0; i < bbList.size(); i++) {
            //Process Each Basic Block

            List<Quad> quadList = bbList.get(i).getQuads();
            for (int j = 0; j < quadList.size(); j++) {
                //We are interested in only these quads
                //System.out.println("Processing Quad:" + quadList.get(j).toString());
                Quad currentMethodQuad = quadList.get(j);
                List<RegisterOperand> definedRegisters = currentMethodQuad.getDefinedRegisters();
                List<RegisterOperand> usedRegisters = currentMethodQuad.getUsedRegisters();

                if (IsReturn(currentMethodQuad) && summary.IsReturnTypeInteresting()) {
                    if (usedRegisters != null && !usedRegisters.isEmpty()) {
                        summary.AddReturnRegistry(usedRegisters.get(0).getRegister());
                    } else {
                        //System.out.println("Possible Compile Error!! Method did not return interesting object");
                    }
                } else {

                    if (IsObjectAllocated(currentMethodQuad)) {
                        //System.out.println("Object allocated");
                        //If we are doing a heap allocation then we will be interested
                        if (currentMethodQuad.getAllOperands().get(1).toString().equals(stateMap.GetConstructorMethod())) {

                            //Get the register to track for
                            Register sourceRegister = definedRegisters.get(0).getRegister();
                            if (trackableInputRegisteries.contains(sourceRegister.toString())) {
                                trackableInputRegisteries.remove(sourceRegister.toString());
                            }
                            //Sanitize the current state maps
                            SanitizeCurrentWorkingRegSet(workingRegSet, typeState, sourceRegister);

                            //Add the Object to our Tracking List
                            TypeStateMap newStateTracker = new TypeStateMap(stateMap);
                            newStateTracker.AddPointsToRegister(sourceRegister);
                            workingRegSet.add(sourceRegister);
                            newStateTracker.TryTransition(stateMap.GetConstructorMethod());
                            typeState.add(newStateTracker);
                            ////System.out.println("Register added to the track registers");
                        }
                    } else if (IsAnyRegisterPresent(workingRegSet, definedRegisters) || HasAnyRegister(trackableInputRegisteries, definedRegisters)) {
                        if (IsObjectMoved(currentMethodQuad)) {
                            if (IsAnyRegisterPresent(workingRegSet, definedRegisters)) {

                                Register targetRegister = definedRegisters.get(0).getRegister();
                                Register sourceRegister = usedRegisters.get(0).getRegister();
                                //Not Sure when this can happen but added for sanity
                                if (!sourceRegister.equals(targetRegister)) {
                                    SanitizeCurrentWorkingRegSet(workingRegSet, typeState, targetRegister);
                                    AddRegisterReference(workingRegSet, typeState, sourceRegister, targetRegister);
                                    if (trackableInputRegisteries.contains(sourceRegister.toString())) {
                                        workingRegSet.remove(sourceRegister);
                                    }
                                    ////System.out.println("Inside Move Instruction of Defined Register");
                                }

                            }
                            if (HasAnyRegister(trackableInputRegisteries, definedRegisters)) {
                                ////System.out.println("Input Register Moved");
                                Register targetRegister = definedRegisters.get(0).getRegister();
                                Register sourceRegister = usedRegisters.get(0).getRegister();

                                TypeStateMap targetTypeStateMap = GetTypeStateForRegister(targetRegister, typeState);
                                if (targetTypeStateMap != null) {
                                    targetTypeStateMap.RemovePointsToRegister(targetRegister);
                                }
                                TypeStateMap sourceTypeStateMap = GetTypeStateForRegister(sourceRegister, typeState);
                                if (sourceTypeStateMap != null) {
                                    sourceTypeStateMap.AddPointsToRegister(targetRegister);
                                }
                                if (targetTypeStateMap != null && targetTypeStateMap.IsTheObjectUnTrackable()) {
                                    typeState.remove(targetTypeStateMap);
                                }
                                if (!workingRegSet.contains(sourceRegister)) {
                                    trackableInputRegisteries.remove(targetRegister.toString());
                                }
                            }
                        } else {
                            //Check for method invocations , state Trasnsition
                            String invokedMethod = GetMethodName(currentMethodQuad);
                            ////System.out.println("Inside State");

                            //So Here we invoked a method
                            if (!invokedMethod.startsWith("<init>") && !callList.contains(invokedMethod)) {
                                ApplyMethodSummary(currentMethodQuad, workingRegSet, typeState, stateMap, callList, summaryMap, true);
                            }



                        }
                    } //As we are doing only 1-CFA..will ignore if a method is called inside another method
                    else if (IsAnyRegisterPresent(workingRegSet, usedRegisters) || HasAnyRegister(trackableInputRegisteries, usedRegisters)) {
                        ////System.out.println("Inside Used of registers");
                        if (HasAnyRegister(trackableInputRegisteries, usedRegisters)) {
                            if (!IsAnyRegisterPresent(workingRegSet, usedRegisters)) {
                                if (IsObjectMoved(currentMethodQuad)) {

                                    Register targetRegister = definedRegisters.get(0).getRegister();
                                    Register sourceRegister = usedRegisters.get(0).getRegister();

                                    SanitizeCurrentWorkingRegSet(workingRegSet, typeState, targetRegister);
                                    workingRegSet.add(targetRegister);

                                    TypeStateMap targetTypeStateMap = GetTypeStateForRegister(sourceRegister, typeState);
                                    if (targetTypeStateMap == null) {
                                        TypeStateMap newStateTracker = new TypeStateMap(stateMap);
                                        //System.out.println("Added Tracker For Assigned input Register");
                                        //System.out.flush();
                                        newStateTracker.AddPointsToRegister(targetRegister);
                                        newStateTracker.AddPointsToRegister(sourceRegister);
                                        typeState.add(newStateTracker);
                                        inputTransformations.put(sourceRegister.toString(), newStateTracker);
                                    } else {
                                        //System.out.println("Added Register:" + targetRegister.toString() + " to tracked items");
                                        targetTypeStateMap.AddPointsToRegister(targetRegister);
                                        //System.out.flush();
                                    }


                                } else {
                                    //Check for method invocations , state Trasnsition
                                    String invokedMethod = GetMethodName(currentMethodQuad);
                                    ////System.out.println("Inside State");
                                    if (invokedMethod != null) {
                                        Register sourceRegister = usedRegisters.get(0).getRegister();
                                        TypeStateMap targetTypeStateMap = GetTypeStateForRegister(sourceRegister, typeState);
                                        //System.out.println("Trying to Make Trasition for:" + invokedMethod);
                                        //System.out.flush();
                                        if (targetTypeStateMap == null) {
                                            //System.out.println("Created a new tracker for Input Argument");
                                            TypeStateMap newStateTracker = new TypeStateMap(stateMap);
                                            newStateTracker.AddPointsToRegister(sourceRegister);
                                            typeState.add(newStateTracker);
                                            inputTransformations.put(sourceRegister.toString(), newStateTracker);
                                            targetTypeStateMap = newStateTracker;
                                            //System.out.flush();
                                        }

                                        if (stateMap.IsValidMethod(invokedMethod)) {
                                            targetTypeStateMap.TryTransition(invokedMethod);
                                        } else {
                                            if (!invokedMethod.startsWith("<init>") && !callList.contains(invokedMethod)) {
                                                ApplyMethodSummary(currentMethodQuad, workingRegSet, typeState, stateMap, callList, summaryMap, true);
                                            }
                                        }

                                    }
                                }
                            }
                        } else if (IsObjectMoved(currentMethodQuad)) {
                            Register targetRegister = definedRegisters.get(0).getRegister();
                            Register sourceRegister = usedRegisters.get(0).getRegister();
                            //Not Sure when this can happen but added for sanity
                            if (!sourceRegister.equals(targetRegister)) {
                                SanitizeCurrentWorkingRegSet(workingRegSet, typeState, targetRegister);
                                AddRegisterReference(workingRegSet, typeState, sourceRegister, targetRegister);
                                ////System.out.println("Register:"+targetRegister.toString() + " added to be traced by:" +sourceRegister.toString());
                            }
                        } else {
                            //Check for method invocations , state Trasnsition
                            String invokedMethod = GetMethodName(currentMethodQuad);

                            if (invokedMethod != null) {
                                if (stateMap.IsValidMethod(invokedMethod)) {
                                    Register sourceRegister = usedRegisters.get(0).getRegister();
                                    TypeStateMap targetTypeStateMap = GetTypeStateForRegister(sourceRegister, typeState);
                                    if (targetTypeStateMap != null) {
                                        //System.out.println("Trying to Add transition:" + currentMethodQuad.toString() + " to registry:" + sourceRegister.toString());
                                        targetTypeStateMap.TryTransition(invokedMethod);
                                    }
                                } else {
                                    if (!invokedMethod.startsWith("<init>") && !callList.contains(invokedMethod)) {
                                        ApplyMethodSummary(currentMethodQuad, workingRegSet, typeState, stateMap, callList, summaryMap, true);
                                    }
                                }

                            }
                        }


                    } else {

                        if (GetMethodName(currentMethodQuad) != null) {
                            String invokedMethod = GetMethodName(currentMethodQuad);
                            jq_Type returnType1 = fullProgram.getMethod(invokedMethod).getReturnType();
                            if (returnType1.getName().equals(stateMap.GetConstructorMethod()) && definedRegisters != null && !definedRegisters.isEmpty()) {
                                ApplyMethodSummary(currentMethodQuad, workingRegSet, typeState, stateMap, callList, summaryMap, true);
                            }

                        }


                    }

                }
                //System.out.flush();
            }
        }

        //System.out.println("No of Input Registers Got Transformed:" + inputTransformations.size());

        //System.out.println("Trying to prepare summary object");
        //System.out.flush();
        //Prepare Summary Object
        //Fill the input transformations
        List<String> inputRegisters = summary.GetInputParameters();
        for (int i = 0; i < inputRegisters.size(); i++) {
            String currentRegister = inputRegisters.get(i);
            if (inputTransformations.containsKey(currentRegister)) {
                summary.AddInputTransformations(currentRegister, inputTransformations.get(currentRegister).GetMethodTrace());
                //System.out.println("Added Input Transformations:" + inputTransformations.get(currentRegister).GetMethodTrace() + " for input register:" + currentRegister.toString());
                //System.out.flush();
            }
        }

        if (summary.IsReturnTypeInteresting()) {
            //Compute the return structures
            Register returnReg = summary.GetReturnRegister();
            TypeStateMap targetTypeState = GetTypeStateForRegister(returnReg, typeState);
            summary.AddReturnTransformations(targetTypeState.GetMethodTrace());
            if (inputTransformations.containsValue(targetTypeState)) {
                for (int i = 0; i < inputRegisters.size(); i++) {
                    String currentRegister = inputRegisters.get(i);
                    if (inputTransformations.containsKey(currentRegister)) {
                        if (inputTransformations.get(currentRegister) == targetTypeState) {
                            summary.AddReturnInputCorrespondance(currentRegister);
                            //System.out.println("Added Return type to input Correspondance for input register:" + currentRegister.toString());
                            //System.out.flush();
                            break;
                        }
                    }
                }
            }
        }

        //System.out.println("Summary Object creation sucessfull");

        //System.out.println("Completed:  Message summary for:" + summary.GetMethodSignature());
        //System.out.flush();
        return summary;
    }

    private Boolean HasAnyRegister(List<String> trackableRegisterNames, List<RegisterOperand> registerOperands) {

        for (int i = 0; i < registerOperands.size(); i++) {
            if (trackableRegisterNames.contains(registerOperands.get(i).getRegister().toString())) {
                return true;
            }
        }
        return false;
    }

    private void GetLoopIncludedBlocks(List<joeq.Compiler.Quad.BasicBlock> bbList,joeq.Compiler.Quad.BasicBlock srcBlock){
        if(srcBlock.getSuccessors() != null && !srcBlock.getSuccessors().isEmpty() && bbList.contains(srcBlock.getSuccessors().get(0))){
            List<joeq.Compiler.Quad.BasicBlock> loopBlocks = new LinkedList<BasicBlock>();
            int index = bbList.indexOf(srcBlock.getSuccessors().get(0));
            for(int i=index;i < bbList.size();i++){
                loopBlocks.add(bbList.get(i));
                System.out.println("Added:" + bbList.get(i).toString());
            }
            bbList.addAll(loopBlocks);
        }
    }

    private List<joeq.Compiler.Quad.BasicBlock> GetBBInDFSOrder(ControlFlowGraph cfg, int loopIterations) {

        List<joeq.Compiler.Quad.BasicBlock> bbList = new LinkedList<joeq.Compiler.Quad.BasicBlock>();
        Map<joeq.Compiler.Quad.BasicBlock, Integer> noOfTimesVisited = new HashMap<joeq.Compiler.Quad.BasicBlock, Integer>();

        joeq.Compiler.Quad.BasicBlock currentBasicBlock = cfg.entry();
        bbList.add(currentBasicBlock);
        int index = 0;
        while (bbList.size() != cfg.getNumberOfBasicBlocks()) {
            if (bbList.size() > index) {
                currentBasicBlock = bbList.get(index);

                List<joeq.Compiler.Quad.BasicBlock> sucessorBasicBlocks = currentBasicBlock.getSuccessors();
                for (int i = 0; i < sucessorBasicBlocks.size(); i++) {
                    if (!bbList.contains(sucessorBasicBlocks.get(i))) {
                        bbList.add(sucessorBasicBlocks.get(i));
                        System.out.println("Added:" + sucessorBasicBlocks.get(i).toString());
                        GetLoopIncludedBlocks(bbList, sucessorBasicBlocks.get(i));
                        try {
							out.write(sucessorBasicBlocks.get(i).fullDump());
							out.write("\n");
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
                        
                    }
                }
            } else {
                break;
            }
            index++;
        }
        return bbList;
    }

    private StateMap TryParseStateSpecFile() {
        StateMap stateMap = null;
        try {
            DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream("StateSpec.txt")));
            stateMap = ParseStateMapFile(in);
        } catch (Exception ex) {
            stateMap = null;
            System.err.println("State Specification File: StateSpec.txt Not Found or Malformed");
            System.err.println("Exiting the Application");
        }
        return stateMap;
    }

    private StateMap ParseStateMapFile(DataInputStream in) throws ArrayIndexOutOfBoundsException {
        StateMap stateMap = new StateMap();
        try {
            while (true) {
                String currentLine = in.readLine();
                if (!currentLine.trim().isEmpty()) {
                    //System.out.println("Processing State Spec:" + currentLine);
                    String[] splitStrings = currentLine.split("-");
                    //System.out.print("MethodName:" + splitStrings[0] + ",intial State:" + splitStrings[1] + ",final State:" + splitStrings[2]);
                    //System.out.println();
                    stateMap.AddMethodTransition(splitStrings[0], splitStrings[1], splitStrings[2]);
                }
            }

        } catch (ArrayIndexOutOfBoundsException e) {
            throw e;
        } catch (Exception e) {
        }
        return stateMap;
    }

    private void ProcessBasicBlock(List<Register> workingRegSet, List<TypeStateMap> currentTypeStateList, StateMap stateMap, joeq.Compiler.Quad.BasicBlock currentBB, Map<String, MethodSummary> methodSummaryMap) {
        List<Quad> quadList = currentBB.getQuads();
        for (int i = 0; i < quadList.size(); i++) {
            //We are interested in only these quads
            //System.out.println("Processing Quad:" + quadList.get(i).toString());
            Quad currentQuad = quadList.get(i);
            List<RegisterOperand> usedRegisters = currentQuad.getUsedRegisters();
            List<RegisterOperand> definedRegisters = currentQuad.getDefinedRegisters();
            if (IsObjectAllocated(currentQuad)) {
                //System.out.println("Object allocated");
                //If we are doing a heap allocation then we will be interested
                if (currentQuad.getAllOperands().get(1).toString().equals(stateMap.GetConstructorMethod())) {
                	System.out.print("Allocated Here:"+currentQuad.toString());
                    //Get the register to track for
                    Register sourceRegister = definedRegisters.get(0).getRegister();

                    //Sanitize the current state maps
                    SanitizeCurrentWorkingRegSet(workingRegSet, currentTypeStateList, sourceRegister);

                    //Add the Object to our Tracking List
                    TypeStateMap newStateTracker = new TypeStateMap(stateMap);
                    newStateTracker.AddPointsToRegister(sourceRegister);
                    workingRegSet.add(sourceRegister);
                    newStateTracker.TryTransition(stateMap.GetConstructorMethod());
                    currentTypeStateList.add(newStateTracker);
                    ////System.out.println("Register:"+sourceRegister.toString()+ " added to the track registers");
                }
            } else if (IsAnyRegisterPresent(workingRegSet, currentQuad.getDefinedRegisters())) {
                if (IsObjectMoved(currentQuad)) {
                    Register targetRegister = definedRegisters.get(0).getRegister();
                    CIObj pointsTo = cipa.pointsTo(targetRegister);
                    try {
						out2.write("Solution Of:"+currentQuad.toString());
						out2.write("\n");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                    
                    if(!pointsTo.pts.isEmpty()){
                    for(Quad q:pointsTo.pts){
                    	try {
                    		FieldOperand op =null;
                    		
							out2.write("Points To Solution:"+q.toString());
							out2.write("\n");
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
                    }
                    }
                    if(!usedRegisters.isEmpty()){
                    Register sourceRegister = usedRegisters.get(0).getRegister();
                    
                    //Not Sure when this can happen but added for sanity
                    if (!sourceRegister.equals(targetRegister)) {
                        SanitizeCurrentWorkingRegSet(workingRegSet, currentTypeStateList, targetRegister);
                        AddRegisterReference(workingRegSet, currentTypeStateList, sourceRegister, targetRegister);
                        ////System.out.println("Inside Move Instruction of Defined Register");
                    }
                    }
                } else {
                    //Check for method invocations , state Trasnsition
                    String invokedMethod = GetMethodName(currentQuad);
                    ////System.out.println("Inside State");
                    if (invokedMethod != null) {
                        if (stateMap.IsValidMethod(invokedMethod)) {

                            /*Register sourceRegister = usedRegisters.get(0).getRegister();
                            TypeStateMap targetTypeStateMap = GetTypeStateForRegister(sourceRegister, currentTypeStateList);
                            if (targetTypeStateMap != null) {
                                System.out.println("Trying to Make Trasition at:"+currentQuad.toString());
                                if (!targetTypeStateMap.TryTransition(invokedMethod)) {
                                    System.out.println("Invalid State Change Detected at:" + currentQuad.toVerboseStr());
                                    this.isProgramValid = false;
                                } else {
                                    System.out.println("current State:" + targetTypeStateMap.GetCurrentState());
                                }
                            }*/
                        } else {
                            //So Here we invoked a method
                            if (!invokedMethod.startsWith("<init>")) {
                                //ApplyMethodSummary(currentQuad, workingRegSet, currentTypeStateList, stateMap, new LinkedList<String>(), methodSummaryMap, false);
                            }


                        }
                    }
                }
            } //Need to handle the method invocations
            else if (IsAnyRegisterPresent(workingRegSet, usedRegisters)) {
                if (IsObjectMoved(currentQuad)) {
                    ////System.out.println("Tracked Register Moved to different register");
                    Register targetRegister = definedRegisters.get(0).getRegister();
                    Register sourceRegister = usedRegisters.get(0).getRegister();
                    //Not Sure when this can happen but added for sanity
                    if (!sourceRegister.equals(targetRegister)) {
                        SanitizeCurrentWorkingRegSet(workingRegSet, currentTypeStateList, targetRegister);
                        AddRegisterReference(workingRegSet, currentTypeStateList, sourceRegister, targetRegister);
                    }
                } else {
                    //Check for method invocations , state Trasnsition
                    String invokedMethod = null;//GetMethodName(currentQuad);
                    //jq_Method meth = Invoke.getMethod(currentQuad).getMethod();
                    
                    Utf8 das = Utf8.get("Hello");
                    
                    //jq_LocalVarTableEntry[] args = meth.getLocalTable();
                    
                    if (invokedMethod != null) {
                        ////System.out.println("Method called on tracked register");
                        /*if (stateMap.IsValidMethod(invokedMethod)) {
                            Register sourceRegister = usedRegisters.get(0).getRegister();
                            TypeStateMap targetTypeStateMap = GetTypeStateForRegister(sourceRegister, currentTypeStateList);
                            if (targetTypeStateMap != null) {
                                System.out.println("Trying to Make Trasition at:"+currentQuad.toString());
                                if (!targetTypeStateMap.TryTransition(invokedMethod)) {
                                    System.out.println("Invalid State Change Detected at:" + currentQuad.toVerboseStr());
                                    this.isProgramValid = false;
                                } else {
                                    System.out.println("current State:" + targetTypeStateMap.GetCurrentState());
                                }
                            }
                        } else {
                            //So Here we invoked a method

                            if (!invokedMethod.startsWith("<init>")) {
                                ApplyMethodSummary(currentQuad, workingRegSet, currentTypeStateList, stateMap, new LinkedList<String>(), methodSummaryMap, false);
                            }
                        }*/


                    }


                }

            } else if (GetMethodName(currentQuad) != null) {
                String invokedMethod = GetMethodName(currentQuad);
                jq_Type returnType = fullProgram.getMethod(invokedMethod).getReturnType();
                if (returnType.getName().equals(stateMap.GetConstructorMethod()) && definedRegisters != null && !definedRegisters.isEmpty()) {
                    //ApplyMethodSummary(currentQuad, workingRegSet, currentTypeStateList, stateMap, new LinkedList<String>(), methodSummaryMap, false);
                }

            }
            //System.out.flush();
        }
    }

    private void ApplyMethodSummary(Quad currentQuad, List<Register> workingRegSet, List<TypeStateMap> typeStateMapList, StateMap map, List<String> callList, Map<String, MethodSummary> summaryMap, Boolean eval) {
        String invokedMethod = GetMethodName(currentQuad);
        if (invokedMethod != null && !callList.contains(invokedMethod)) {
            callList.add(invokedMethod);
            if (!summaryMap.containsKey(invokedMethod)) {
                summaryMap.put(invokedMethod, GetMethodSummary(currentQuad, map, callList, summaryMap));
            }
            //System.out.println("Trying to Apply Method Summary:");
            MethodSummary targetMethodSummary = summaryMap.get(invokedMethod);

            Map<String, Register> inputParameterRegisterMap = new HashMap<String, Register>();
            List<RegisterOperand> usedRegisters = currentQuad.getUsedRegisters();
            List<RegisterOperand> definedRegisters = currentQuad.getDefinedRegisters();

            for (int i = 0; i < usedRegisters.size(); i++) {
                Register sourceRegister = usedRegisters.get(i).getRegister();

                List<String> transforms = targetMethodSummary.GetInputTransformation("R" + i);
                inputParameterRegisterMap.put("R" + i, sourceRegister);

                if (transforms != null && !transforms.isEmpty()) {
                    TypeStateMap targetTypeStateMap = GetTypeStateForRegister(sourceRegister, typeStateMapList);
                    if (targetTypeStateMap != null) {
                        //Get the transformations that are done to the input arguments
                        //Apply these transformations to the input arguments
                        for (int j = 0; j < transforms.size(); j++) {
                            String currentTransform = transforms.get(j);
                            System.out.println("Trying to Make Trasition at:"+currentQuad.toString());
                            if (!targetTypeStateMap.TryTransition(currentTransform) && !eval) {
                                System.out.println("Invalid State Change Detected at:" + currentQuad.toVerboseStr());
                                this.isProgramValid = false;
                            } else {
                                System.out.println("current State:" + targetTypeStateMap.GetCurrentState());
                            }
                        }
                    }
                }
            }

            if (targetMethodSummary.IsReturnTypeInteresting() && definedRegisters != null && !definedRegisters.isEmpty()) {
                if (targetMethodSummary.GetReturnInputCorrespondance() != null) {
                    Register sourceRegister = inputParameterRegisterMap.get(targetMethodSummary.GetReturnInputCorrespondance());

                    Register targetRegister = definedRegisters.get(0).getRegister();
                    if (!sourceRegister.equals(targetRegister)) {
                        SanitizeCurrentWorkingRegSet(workingRegSet, typeStateMapList, targetRegister);
                        AddRegisterReference(workingRegSet, typeStateMapList, sourceRegister, targetRegister);
                        //System.out.println("Assigned Input Register :" + sourceRegister.toString() + " to destination register:" + targetRegister.toString());
                    }
                } else {
                    Register targetRegister = definedRegisters.get(0).getRegister();
                    //a new object is created in the method and that was returned
                    //So Mimic the state Transitions on the object that needs to the tracked
                    SanitizeCurrentWorkingRegSet(workingRegSet, typeStateMapList, targetRegister);

                    //Add the Object to our Tracking List
                    //System.out.println("Trying to Mimic the functions called on this object in the function");
                    TypeStateMap newStateTracker = new TypeStateMap(map);
                    newStateTracker.AddPointsToRegister(targetRegister);
                    workingRegSet.add(targetRegister);
                    typeStateMapList.add(newStateTracker);
                    //System.out.println("Current State:" + newStateTracker.GetCurrentState());
                    //Do the transitions traced
                    for (int i = 0; i < targetMethodSummary.GetReturnTransformations().size(); i++) {
                        System.out.println("Trying to mimic method:" + targetMethodSummary.GetReturnTransformations().get(i));
                        if (!newStateTracker.TryTransition(targetMethodSummary.GetReturnTransformations().get(i)) && !eval) {
                            System.out.println("Invalid State Change Occured in the method:" + invokedMethod + " @" + currentQuad.toVerboseStr());
                            this.isProgramValid = false;
                        }
                        System.out.println("Current State:" + newStateTracker.GetCurrentState());
                    }
                }
                //System.out.flush();
            }
            //System.out.flush();
        }
        //System.out.flush();
    }

    private TypeStateMap GetTypeStateForRegister(Register r, List<TypeStateMap> typeStateMapList) {
        TypeStateMap targetTypeState = null;
        for (int i = 0; i < typeStateMapList.size(); i++) {
            if (typeStateMapList.get(i).IsInPointsToRegister(r)) {

                targetTypeState = typeStateMapList.get(i);
                break;
            }
        }

        return targetTypeState;
    }

    private void AddRegisterReference(List<Register> currWorkingSetRegisters, List<TypeStateMap> currentTypeStateMap, Register sourceRegister, Register targetRegister) {
        if (currWorkingSetRegisters.contains(sourceRegister) && !sourceRegister.equals(targetRegister)) {

            for (int i = 0; i < currentTypeStateMap.size(); i++) {

                TypeStateMap workingTypeState = currentTypeStateMap.get(i);
                if (workingTypeState.IsInPointsToRegister(sourceRegister)) {

                    workingTypeState.AddPointsToRegister(targetRegister);
                    currWorkingSetRegisters.add(targetRegister);
                    ////System.out.println("Added register:"+ targetRegister.toString()+ " to be the tracked");
                    break;
                }
            }
        }
    }

    private void SanitizeCurrentWorkingRegSet(List<Register> currWorkingSetRegisters, List<TypeStateMap> currentTypeStateMap, Register targetRegister) {
        List<TypeStateMap> unTrackableObjects = new LinkedList<TypeStateMap>();
        if (currWorkingSetRegisters.contains(targetRegister)) {

            for (int i = 0; i < currentTypeStateMap.size(); i++) {

                TypeStateMap workingTypeState = currentTypeStateMap.get(i);
                workingTypeState.RemovePointsToRegister(targetRegister);

                if (workingTypeState.IsTheObjectUnTrackable()) {

                    unTrackableObjects.add(workingTypeState);

                }
            }
            ////System.out.println("Register:"+targetRegister.toString()+" removed from tracked register!!");
            currWorkingSetRegisters.remove(targetRegister);
        }
        currentTypeStateMap.removeAll(unTrackableObjects);
    }

    private Boolean IsObjectAllocated(Quad currentQuad) {
        return currentQuad.getOperator().toString().equals("NEW");
    }

    private Boolean IsObjectMoved(Quad currentQuad) {
        return currentQuad.getOperator().toString().equals("MOVE_A");
    }

    private Boolean IsReturn(Quad currentQuad) {
        return currentQuad.getOperator().toString().contains("RETURN");
    }

    private Boolean IsMethodInvokeInteresting(Quad currentQuad) {
        return currentQuad.getOperator().toString().startsWith("INVOKE");
    }

    private String GetMethodName(Quad currentQuad) {
        String methodName = null;
        if (IsMethodInvokeInteresting(currentQuad)) {
            methodName = Operator.Invoke.getMethod(currentQuad).toString();
            System.out.println("Bingo::");
        }
        return methodName;
    }

    private Boolean IsAnyRegisterPresent(List<Register> workingReg, List<RegisterOperand> operandList) {
        if (operandList != null) {
            for (int i = 0; i < operandList.size(); i++) {
                if (workingReg.contains(operandList.get(i).getRegister())) {
                    return true;
                }
            }
        }
        return false;
    }
}

