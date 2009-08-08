# On Windows, uses Cygwin b20.1 tools with Mingw runtime. 
# 
# Things you may need to change, or redefine on the command line:
#   JDK_ROOT    -- location where you installed JDK
#   M32_FLAG    -- "-m32" if using 32-bit JVM on 64-bit machine

LIB_DIR = ../lib

ifeq (${OS},Windows_NT)
  CXXFLAGS = $(M32_FLAG) -mno-cygwin -Wall -O2
  OBJECT_OUTPUT_OPTION = -o$(space)
  LINK = dllwrap
  LINKFLAGS = $(M32_FLAG) -s --target=i386-mingw32 --add-underscore --driver-name c++ -mno-cygwin
  DLL_OUTPUT_OPTION = -o$(space)
  INCLUDES = -I"$(JDK_ROOT)/include" \
             -I"$(JDK_ROOT)/include/win32"
  TRACE_DLL_NAME = $(LIB_DIR)/chord_trace_agent.dll
  INSTR_DLL_NAME = $(LIB_DIR)/chord_instr_agent.dll
else
  ifeq (${shell uname -s},Darwin)
    CXXFLAGS = $(M32_FLAG) -fPIC -DSPECIALIZE_RELPROD -DSPECIALIZE_AND -DSPECIALIZE_OR -DSMALL_NODES -O2 -fomit-frame-pointer
    OBJECT_OUTPUT_OPTION = -o$(space)
    LINK = $(CXX) 
    LINKFLAGS = $(M32_FLAG) -dynamiclib -framework JavaVM  -single_module -undefined suppress -flat_namespace
    DLL_OUTPUT_OPTION = -o$(space)
    INCLUDES = -I$(JDK_ROOT)/Headers/
    TRACE_DLL_NAME = $(LIB_DIR)/libchord_trace_agent.jnilib
    INSTR_DLL_NAME = $(LIB_DIR)/libchord_instr_agent.jnilib
  else
    CXXFLAGS = $(M32_FLAG) -fPIC -DSPECIALIZE_RELPROD -DSPECIALIZE_AND -DSPECIALIZE_OR -DSMALL_NODES -O2 -fomit-frame-pointer
    OBJECT_OUTPUT_OPTION = -o$(space)
    LINK = $(CXX)
    LINKFLAGS = $(M32_FLAG) -shared
    DLL_OUTPUT_OPTION = -o$(space)
    INCLUDES = -I. -I$(JDK_ROOT)/include \
               -I$(JDK_ROOT)/include/linux
    TRACE_DLL_NAME = $(LIB_DIR)/libchord_trace_agent.so
    INSTR_DLL_NAME = $(LIB_DIR)/libchord_instr_agent.so
  endif
endif

default: $(TRACE_DLL_NAME) $(INSTR_DLL_NAME)

$(TRACE_DLL_NAME): chord_trace_agent.o
	$(LINK) $(LINKFLAGS) $< $(DLL_OUTPUT_OPTION)$@

$(INSTR_DLL_NAME): chord_instr_agent.o
	$(LINK) $(LINKFLAGS) $< $(DLL_OUTPUT_OPTION)$@

chord_trace_agent.o: chord_trace_agent.cpp
	$(CXX) chord_trace_agent.cpp $(CXXFLAGS) $(INCLUDES) -c $(OBJECT_OUTPUT_OPTION)$@

chord_instr_agent.o: chord_instr_agent.cpp
	$(CXX) chord_instr_agent.cpp $(CXXFLAGS) $(INCLUDES) -c $(OBJECT_OUTPUT_OPTION)$@

clean:
	$(RM) -f chord_trace_agent.o chord_instr_agent.o $(TRACE_DLL_NAME) $(INSTR_DLL_NAME)

empty := 
space := $(empty) $(empty)
