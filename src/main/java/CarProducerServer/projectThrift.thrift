namespace java src.CarProducerServer

typedef string str

service TransportService {
	void write(1:list<str> n1),
}