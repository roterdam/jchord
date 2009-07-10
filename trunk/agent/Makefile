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
  DLL_NAME = $(LIB_DIR)/tracing_agent.dll
else
  ifeq (${shell uname -s},Darwin)
    CXXFLAGS = $(M32_FLAG) -fPIC -DSPECIALIZE_RELPROD -DSPECIALIZE_AND -DSPECIALIZE_OR -DSMALL_NODES -O2 -fomit-frame-pointer
    OBJECT_OUTPUT_OPTION = -o$(space)
    LINK = $(CXX) 
    LINKFLAGS = $(M32_FLAG) -dynamiclib -framework JavaVM  -single_module -undefined suppress -flat_namespace
    DLL_OUTPUT_OPTION = -o$(space)
    INCLUDES = -I$(JDK_ROOT)/Headers/
    DLL_NAME = $(LIB_DIR)/libtracing_agent.jnilib
  else
    CXXFLAGS = $(M32_FLAG) -fPIC -DSPECIALIZE_RELPROD -DSPECIALIZE_AND -DSPECIALIZE_OR -DSMALL_NODES -O2 -fomit-frame-pointer
    OBJECT_OUTPUT_OPTION = -o$(space)
    LINK = $(CXX)
    LINKFLAGS = $(M32_FLAG) -shared
    DLL_OUTPUT_OPTION = -o$(space)
    INCLUDES = -I. -I$(JDK_ROOT)/include \
               -I$(JDK_ROOT)/include/linux
    DLL_NAME = $(LIB_DIR)/libtracing_agent.so
  endif
endif

default: $(DLL_NAME)

$(DLL_NAME): tracing_agent.o
	$(LINK) $(LINKFLAGS) $< $(DLL_OUTPUT_OPTION)$@

tracing_agent.o: tracing_agent.cpp
	$(CXX) tracing_agent.cpp $(CXXFLAGS) $(INCLUDES) -c $(OBJECT_OUTPUT_OPTION)$@

clean:
	$(RM) -f tracing_agent.o $(DLL_NAME)

empty := 
space := $(empty) $(empty)
