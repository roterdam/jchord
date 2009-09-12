# On Windows, uses Cygwin b20.1 tools with Mingw runtime. 
# 
# Things you may need to change, or redefine on the command line:
#   JDK_ROOT    -- location where you installed JDK
#   M32_FLAG    -- "-m32" if using 32-bit JVM on 64-bit machine

LIB_DIR = ../lib
JNI_H_DIR = $(shell find $(JDK_ROOT) -name jni.h -exec dirname '{}' \;)
JNI_MD_H_DIR = $(shell find $(JDK_ROOT) -name jni_md.h -exec dirname '{}' \;)
INCLUDES = -I. -I$(JNI_H_DIR) -I$(JNI_MD_H_DIR) -I$(BUDDY_SRC) -I$(BUDDY_SRC)/..
DLL_OUTPUT_OPTION = -o$(space)
OBJECT_OUTPUT_OPTION = -o$(space)

ifeq (${OS},Windows_NT)
  CXXFLAGS = $(M32_FLAG) -mno-cygwin -Wall -O2
  LINK = dllwrap
  LINKFLAGS = $(M32_FLAG) -s --target=i386-mingw32 --add-underscore --driver-name c++ -mno-cygwin
  INSTR_DLL_NAME = $(LIB_DIR)/chord_instr_agent.dll
else
  ifeq (${shell uname -s},Darwin)
    CXXFLAGS = $(M32_FLAG) -fPIC -DSPECIALIZE_RELPROD -DSPECIALIZE_AND -DSPECIALIZE_OR -DSMALL_NODES -O2 -fomit-frame-pointer
    LINK = $(CXX) 
    LINKFLAGS = $(M32_FLAG) -dynamiclib -framework JavaVM  -single_module -undefined suppress -flat_namespace
    INSTR_DLL_NAME = $(LIB_DIR)/libchord_instr_agent.jnilib
  else
    CXXFLAGS = $(M32_FLAG) -fPIC -DSPECIALIZE_RELPROD -DSPECIALIZE_AND -DSPECIALIZE_OR -DSMALL_NODES -O2 -fomit-frame-pointer
    LINK = $(CXX)
    LINKFLAGS = $(M32_FLAG) -shared
    INSTR_DLL_NAME = $(LIB_DIR)/libchord_instr_agent.so
  endif
endif

default: $(INSTR_DLL_NAME)

$(INSTR_DLL_NAME): chord_instr_agent.o
	$(LINK) $(LINKFLAGS) $< $(DLL_OUTPUT_OPTION)$@

chord_instr_agent.o: chord_instr_agent.cpp
	$(CXX) chord_instr_agent.cpp $(CXXFLAGS) $(INCLUDES) -c $(OBJECT_OUTPUT_OPTION)$@

clean:
	$(RM) -f chord_instr_agent.o $(INSTR_DLL_NAME)

empty := 
space := $(empty) $(empty)
