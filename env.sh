if [ ! -s .env ]; then
    cp .env.dist .env
	echo "Your .env file is created! Now please edit it to set right environment config."
else
	echo "Your .env file was already created."
fi
