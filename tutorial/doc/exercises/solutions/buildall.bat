for /L %%i in (1,1,10) do (
cd exercise%%i
call build
cd ..
)
