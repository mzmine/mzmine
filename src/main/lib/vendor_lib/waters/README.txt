In Visual C++, it is necessary to set 

C/C++ -> Code Generation -> Runtime Library -> Multi-threaded (MT)

Otherwise, DLL hell.

Note that masslynxraw.dll still depends on msvcp120.dll and msvcr120.dll!

