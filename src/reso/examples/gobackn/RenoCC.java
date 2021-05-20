package reso.examples.gobackn;

import reso.examples.gobackn.GBNCCProtocol.SENDER;

public class RenoCC {
	
	private float _cwnd;
	private boolean _slowStart;
	private int _lastACKSqNb;
	private int _repeatedACK;
	private static final int MAX_DUP_ACK = 3;
	private static final int DUP_ACK_CWND_DIVIDE = 2;
	private float _ssthresh;
	private final GBNCCProtocol _proto;
	private boolean _fastRC;
	private int _fastRCToReceive;
	
	public RenoCC(GBNCCProtocol proto){
		_proto = proto;
		reset();
	}
	
	public void timeout(){
		_proto.log(false, SENDER.BOTH, "Slow start on");
		setWindowSize(1.0f);
		_slowStart = true;
	}
	
	public void reset(){
		_cwnd = 1.0f;
		_slowStart = true;
		_lastACKSqNb = -1;
		_repeatedACK = 1;
		_ssthresh = 20;
		_fastRC = false;
		_fastRCToReceive = -1;
	}
	
	public int getWindowSize(){
		return (int)_cwnd;
	}
	
	public void setWindowSize(float size){
		if(getWindowSize() != (int)size){
			_proto.log(false, SENDER.BOTH, "Cwnd " + getWindowSize() + " => " + (int)(size));
		}
		_cwnd = size;
		if(_cwnd < 1.0f) _cwnd = 1;
	}

	public void receiveACK(int sqNb) {	
		
		if(_fastRC && sqNb == _fastRCToReceive){
			_fastRC = false;
		}
		
		if(sqNb == _lastACKSqNb && !_fastRC){ // Duplicate ACK
			_repeatedACK++;
			if(_repeatedACK == MAX_DUP_ACK){ // 3 duplicate ACK exactly
				
				_proto.log(true, SENDER.SENDER, "3 duplicate ACK (" + sqNb + ")");
				
				_ssthresh = _cwnd / DUP_ACK_CWND_DIVIDE; // threshold equals to half of the congestion window when loss occurs.
				
				setWindowSize(_ssthresh);
				
				_fastRC = true;
				_fastRCToReceive = _proto.getLastWindowIndex();

				_proto.stopTimer();
				_proto.timeout(false);				
			}else if(_repeatedACK > MAX_DUP_ACK){ // More than 3 dup ack by rfc5681 9.4
				setWindowSize(_cwnd + 1);
			}
		}else{ // Different ACK
			_lastACKSqNb = sqNb;
			_repeatedACK = 1;
			
			if(_slowStart){ 
				setWindowSize(_cwnd + 1); // Grow exponentially
				if(_cwnd >= _ssthresh){ // cwnd is now beyond the threshold, disable SS
					_slowStart = false;
					_proto.log(false, SENDER.BOTH, "Slow start off");
				}
			}else{
				_proto.log(false, SENDER.BOTH, "Additive increase");
				float wSizeMSS = GBNCCProtocol.MSS * _cwnd;
				float newSizeMSS = wSizeMSS + (float)(Math.pow(GBNCCProtocol.MSS, 2) / wSizeMSS);
				setWindowSize(newSizeMSS / GBNCCProtocol.MSS);
			}
			
		}		
	}
}
