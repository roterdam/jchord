#
# Makefile to create Java JNI library.
# On Windows, uses Cygwin b20.1 tools with Mingw runtime. 
# 
# Things you may need to change, or redefine on the command line:
#   JDK_ROOT     -- location where you installed JDK
#   M32_FLAG     -- "-m32" if using 32-bit JVM on 64-bit machine

BUDDY_SRC = buddy/src
LIB_DIR = ../lib

ifeq (${OS},Windows_NT)
  CFLAGS = $(M32_FLAG) -fPIC -Wall -O2 -mno-cygwin
  OBJECT_OUTPUT_OPTION = -o$(space)
  LINK = dllwrap
  LINKFLAGS = $(M32_FLAG) -s --target=i386-mingw32 --add-underscore --driver-name gcc -mno-cygwin
  DLL_OUTPUT_OPTION = -o$(space)
  INCLUDES = -I. -I"$(JDK_ROOT)/include" \
             -I$(BUDDY_SRC) -I$(BUDDY_SRC)/.. \
             -I"$(JDK_ROOT)/include/win32"
  BUDDY_DLL_NAME = $(LIB_DIR)/buddy.dll
else
  ifeq (${shell uname -s},Darwin)
    CFLAGS = $(M32_FLAG) -fPIC -DSPECIALIZE_RELPROD -DSPECIALIZE_AND -DSPECIALIZE_OR -DSMALL_NODES -O2 -fomit-frame-pointer
    OBJECT_OUTPUT_OPTION = -o$(space)
    LINK = $(CC) 
    LINKFLAGS = $(M32_FLAG) -dynamiclib -framework JavaVM  -single_module -undefined suppress -flat_namespace
    DLL_OUTPUT_OPTION = -o$(space)
    INCLUDES = -I. -I$(JDK_ROOT)/Headers/ \
               -I$(BUDDY_SRC) -I$(BUDDY_SRC)/..
    BUDDY_DLL_NAME = $(LIB_DIR)/libbuddy.jnilib
  else
    CFLAGS = $(M32_FLAG) -fPIC -DSPECIALIZE_RELPROD -DSPECIALIZE_AND -DSPECIALIZE_OR -DSMALL_NODES -O2 -fomit-frame-pointer
    OBJECT_OUTPUT_OPTION = -o$(space)
    LINK = $(CC)
    LINKFLAGS = $(M32_FLAG) -shared
    DLL_OUTPUT_OPTION = -o$(space)
    INCLUDES = -I. -I$(JDK_ROOT)/include \
               -I$(BUDDY_SRC) -I$(BUDDY_SRC)/.. \
               -I$(JDK_ROOT)/include/linux
    BUDDY_DLL_NAME = $(LIB_DIR)/libbuddy.so
  endif
endif

BUDDY_INCLUDE = buddy_jni.h
BUDDY_SRCS = buddy_jni.c \
	$(BUDDY_SRC)/bddio.c $(BUDDY_SRC)/bddop.c $(BUDDY_SRC)/bvec.c \
	$(BUDDY_SRC)/cache.c $(BUDDY_SRC)/fdd.c $(BUDDY_SRC)/imatrix.c \
	$(BUDDY_SRC)/kernel.c $(BUDDY_SRC)/pairs.c $(BUDDY_SRC)/prime.c \
	$(BUDDY_SRC)/reorder.c $(BUDDY_SRC)/tree.c $(BUDDY_SRC)/trace.c
BUDDY_OBJS = $(BUDDY_SRCS:.c=.o)

default: $(BUDDY_DLL_NAME)

$(BUDDY_DLL_NAME): $(BUDDY_OBJS)
	$(LINK) $(DLL_OUTPUT_OPTION)$@ $(BUDDY_OBJS) $(LINKFLAGS)

buddy_jni.o: $(BUDDY_INCLUDE)

.c.o:
	$(CC) $(CFLAGS) $(INCLUDES) -c $(OBJECT_OUTPUT_OPTION)$@ $<

clean:
	$(RM) -f $(BUDDY_OBJS) $(BUDDY_DLL_NAME)

empty := 
space := $(empty) $(empty)
