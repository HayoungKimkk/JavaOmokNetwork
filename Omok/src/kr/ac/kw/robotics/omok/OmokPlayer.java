package kr.ac.kw.robotics.omok;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class OmokPlayer extends Thread{
		
	private static final int EMPTY = 0;
	private static final int BLACK = 1;
	private static final int WHITE = 2;
	
	
	private static OmokGui omokGui;
	private static OmokMap omokMap = new OmokMap();
	
	private static int myStone;
	private static int enemyStone;
	private static String myStoneStr;
	
	private static boolean isMyTurn = false;

	static DataInputStream dis;
	static DataOutputStream dos;
	static Socket socket;
    
	public OmokPlayer(String host, int port)
	{
		System.out.println("[C] 서버(" + host + ":" + port + ")에 연결 중...");
		
		try 
		{
			socket = new Socket(host, port);
			dis = new DataInputStream(socket.getInputStream());
	        dos = new DataOutputStream(socket.getOutputStream());
		} 
		catch (UnknownHostException e) 
		{
			omokGui.setTurn("[ERROR]: 서버가 닫혀있습니다.");
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			omokGui.setTurn("[ERROR]: 서버가 닫혀있습니다.");
			e.printStackTrace();
		}

	}
	
	public static class OmokMap
	{
		public static final int COLS = 15;
		public static final int ROWS = 15;

		private static int map[][];
		
		public OmokMap()
		{
			map = new int[ROWS][];
			for (int i = 0; i < ROWS; i++) 
			{
				map[i] = new int[COLS];
			}
			
			for (int i = 0; i < ROWS; i++) 
			{
				for (int j = 0; j < COLS; j++)
				{
					map[i][j] = EMPTY;
				}
			}
		}
		
		public boolean setMap(int col,int row, int stone)
		{
			if(col>=0 && col<COLS)
				if(row>=0 && row<ROWS)
				{
					map[col][row] = stone;
					
					if(myStone == BLACK && stone == BLACK)
					{
						if(checkRule(col, row) == false)
						{
							map[col][row] = EMPTY;
							return false;
						}
					}
					
					return true;
				}
			return false;
		}
		
		public boolean checkRule(int col, int row)
		{
			// 33 44 6목 검사
			int[] nPointArr = new int[]{0,0,0,0};
			final int DIRECTION = 4;
			//1. 수평 방향
			for(int startRow = row - 5; startRow<= row; startRow++)
			{
				int nPoint  = 0;
				
				for(int moveRow = startRow; moveRow<= startRow+5; moveRow++)//6목 까지 검사
				{
					if(getMap(col, moveRow) == BLACK)
					{
						nPoint++;
					}
					else
					{
						break;
					}
				}
				
				if(nPointArr[0] < nPoint)
				{
					nPointArr[0] = nPoint;
				}
			}
			
			//수직방향
			for(int startCol = col - 5; startCol<= col; startCol++)
			{
				int nPoint  = 0;
				
				for(int moveCol = startCol; moveCol<= startCol+5; moveCol++)
				{
					if(getMap(moveCol, row) == BLACK)
					{
						nPoint++;
					}
					else
					{
						break;
					}
				}
				
				if(nPointArr[1] < nPoint)
				{
					nPointArr[1] = nPoint;
				}
			}
			
			//3. 좌 -> 우 하강  대각선 방향
			for(int nStartMove = -5; nStartMove <= 0; nStartMove++)
			{
				int startRow = row + nStartMove;
				int startCol = col + nStartMove;
				
				int nPoint  = 0;
				
				for(int nEndMove = 0; nEndMove <= 5; nEndMove++)
				{
					int moveRow = startRow + nEndMove;
					int moveCol = startCol + nEndMove;
					
					if(getMap(moveCol, moveRow) == BLACK)
					{
						nPoint++;
					}
					else
					{
						break;
					}
				}

				if(nPointArr[2] < nPoint)
				{
					nPointArr[2] = nPoint;
				}
			}

			//4.좌 -> 우 상승 대각선 방향
			for(int nStartMove = -5; nStartMove <= 0; nStartMove++)
			{
				int startRow = row + nStartMove;
				int startCol = col - nStartMove;
				
				int nPoint  = 0;
				
				for(int nEndMove = 0; nEndMove <= 5; nEndMove++)
				{
					int moveRow = startRow + nEndMove;
					int moveCol = startCol - nEndMove;
					
					if(getMap(moveCol, moveRow) == BLACK)
					{
						nPoint++;
					}
					else
					{
						break;
					}
				}

				if(nPointArr[3] < nPoint)
				{
					nPointArr[3] = nPoint;
				}
			}
			
			int cntThree = 0;
			int cntFour = 0;
			
			for(int i = 0; i<DIRECTION; i++)
			{
				if(nPointArr[i] == 3)
				{
					cntThree++;
				}
				else if(nPointArr[i] == 4)
				{
					cntFour++;
				}
				else if(nPointArr[i]>5) //장목
				{
					JOptionPane.showMessageDialog( omokGui, "[경고] 규칙 위반!! 장목");
					return false;
				}
			}
			
			if(cntThree >= 2)
			{
				JOptionPane.showMessageDialog( omokGui, "[경고] 규칙 위반!! 3 3");
				return false;
			}
			
			if(cntFour >= 2)
			{
				JOptionPane.showMessageDialog( omokGui, "[경고] 규칙 위반!! 4 4");
				return false;
			}

			return true;
		}
		
		public int getMap(int col,int row)
		{
			if(col>=0 && col<COLS)
				if(row>=0 && row<ROWS)
				{
					return map[col][row];
				}
			
			return -1;
		}
	}
	
	public static class OmokGui extends JFrame
	{
		private static final int GUI_WIDTH = 660;
		private static final int GUI_HEIGHT = 750;
		
		private Omokboard omokboard  = new Omokboard();
		private JPanel panelState = new JPanel();
		private JPanel panelChance = new JPanel();
		
		private JLabel labelTurnState = new JLabel("Wait...");
		
		public OmokGui(String title)
		{
			Container c = getContentPane();
			
			
			Dimension dim = new Dimension(GUI_WIDTH, GUI_HEIGHT);
			setTitle(title);
			
			setLocation(300,300);
			setPreferredSize(dim);
				
			
			labelTurnState.setSize(30, 10);  //크키 설정
			labelTurnState.setLocation(Omokboard.BOARD_X
									, Omokboard.BOARD_Y + Omokboard.BOARD_GUI_SIZE + Omokboard.BOARD_CELL); //위치 설정
			panelState.add(labelTurnState);
			
			JButton btnChanceA = new JButton("찬스 1"); // 버튼 초기화
			JButton btnChanceB = new JButton("찬스 2"); // 버튼 초기화
			
			btnChanceA.addActionListener(new ActionListener() {
				boolean isUsed = false;
			    public void actionPerformed(ActionEvent e) {
			        if(isMyTurn == true && isUsed == false)
			        {
			        	ArrayList<Integer> listEmptyCol = new ArrayList<Integer>();
			        	ArrayList<Integer> listEmptyRow = new ArrayList<Integer>();
			        	
			        	listEmptyCol.clear();
			        	listEmptyRow.clear();
			        	
			        	for (int i = 0; i < OmokMap.ROWS; i++) 
						{
							for (int j = 0; j < OmokMap.COLS; j++)
							{
								if(omokMap.getMap(i, j) == 0)
								{
									listEmptyCol.add(i);
									listEmptyRow.add(j);
								}
							}
						}

			        	if(listEmptyCol.size() == listEmptyRow.size() && listEmptyRow.size() != 0)
			        	{
			        		Random random = new Random(System.currentTimeMillis());
			        		
			        		int emptyStoneIdx = Math.abs(random.nextInt())%listEmptyRow.size();

			        		int col = listEmptyCol.get(emptyStoneIdx);
			        		int row = listEmptyRow.get(emptyStoneIdx);
			        		
			        		if(omokMap.setMap(col, row, myStone) == true)
			        		{
				        		try 
				        		{
									dos.writeUTF("CHANCE");
									
									dos.writeInt(col);
									dos.writeInt(row);
									dos.writeInt(myStone);
									
									dos.flush();
								} 
				        		catch (IOException e1) 
				        		{
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
				        		
				        		omokGui.updateBoard();
				        		
					        	isUsed = true;
			        		}
			        	}
			        }
			    }
			});
			
			btnChanceB.addActionListener(new ActionListener() {
				boolean isUsed = false;
			    public void actionPerformed(ActionEvent e) {
			        if(isMyTurn == true  && isUsed == false)
			        {
			        	List<Integer> listExistCol = new ArrayList<Integer>();
			        	List<Integer> listExistRow = new ArrayList<Integer>();
			        	
			        	
			        	for (int i = 0; i < OmokMap.ROWS; i++) 
						{
							for (int j = 0; j < OmokMap.COLS; j++)
							{
								if(omokMap.getMap(i, j) == enemyStone)
								{
									listExistCol.add(i);
									listExistRow.add(j);
								}
							}
						}
			        	if(listExistCol.size() == listExistRow.size() && listExistRow.size() != 0)
			        	{
			        		Random random = new Random(System.currentTimeMillis());
			        		
			        		int existStoneIdx = Math.abs(random.nextInt())%listExistRow.size();
			        		
			        		int col = listExistCol.get(existStoneIdx);
			        		int row = listExistRow.get(existStoneIdx);
			        		
			        		try 
			        		{
								dos.writeUTF("CHANCE");
								dos.writeInt(col);
								dos.writeInt(row);
								dos.writeInt(EMPTY); // 지우기
								dos.flush();
							} 
			        		catch (IOException e1) 
			        		{
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
			        		
			        		omokMap.setMap(col, row, EMPTY);
			        		omokGui.updateBoard();
			        		
				        	isUsed = true;
			        	}
			        }
			    }
			});


			panelChance.setLayout(new FlowLayout());
			panelChance.add(btnChanceA);
			panelChance.add(btnChanceB);
			
			c.setLayout(new BorderLayout());
			
			c.add(omokboard,  BorderLayout.CENTER);
			c.add(panelState, BorderLayout.NORTH);
			c.add(panelChance, BorderLayout.SOUTH);

			pack();
		}
		
		public void updateBoard()
		{
			omokboard.repaint();
		}
		
		public void setTurn(String turn)
		{
			labelTurnState.setText(turn);
			this.revalidate();
		}
		
		public class Omokboard extends JPanel
		{
			public static final int BOARD_SIZE = 15;
			public static final int STONE_GUI_SIZE = 15; //x좌표 시작점
			
			public static final int BOARD_X = 50; //x좌표 시작점
			public static final int BOARD_Y = 50; //x좌표 시작점
			
			public static final int BOARD_GUI_SIZE = GUI_WIDTH-(BOARD_X*2);
			public static final int BOARD_CELL 	= BOARD_GUI_SIZE/(BOARD_SIZE-1);
			
			public Omokboard()
			{
				setPreferredSize(new Dimension(GUI_WIDTH,BOARD_GUI_SIZE));
				this.setBackground(new Color(206,167,61));
				this.addMouseListener(new MyMouseEvent());
			}
			
			@Override
			public void paint(Graphics g)
			{
				super.paintComponent(g);
				drawCheckboard(g);
				drawStone(g);
			}
			
			void drawCheckboard(Graphics g)
			{			
				for(int i= 0; i<BOARD_SIZE; i++)
				{
					//가로 줄
					g.drawLine(BOARD_X
							, BOARD_Y + i*BOARD_CELL 
							, BOARD_X + BOARD_GUI_SIZE
							, BOARD_Y + i*BOARD_CELL); 
					//세로 줄
					g.drawLine(BOARD_X + i*BOARD_CELL
							, BOARD_Y
							, BOARD_X + i*BOARD_CELL 
							, BOARD_Y + BOARD_GUI_SIZE); 
				}
			}
			
			void drawStone(Graphics g)
			{
				for (int i = 0; i < OmokMap.ROWS; i++) 
				{
					for (int j = 0; j < OmokMap.COLS; j++)
					{
						switch(omokMap.getMap(i, j))
						{
						case BLACK:
							g.setColor(Color.BLACK);
							g.fillOval((BOARD_X + j*BOARD_CELL) - STONE_GUI_SIZE
									, (BOARD_Y + i*BOARD_CELL) -STONE_GUI_SIZE
									, STONE_GUI_SIZE*2
									, STONE_GUI_SIZE*2);
							break;
						case WHITE:
							g.setColor(Color.WHITE);
							g.fillOval((BOARD_X + j*BOARD_CELL) - STONE_GUI_SIZE
									, (BOARD_Y + i*BOARD_CELL) -STONE_GUI_SIZE
									, STONE_GUI_SIZE*2
									, STONE_GUI_SIZE*2);
							break;
						default:
							break;
						}
					}
				}
			}
			
			 class MyMouseEvent implements MouseListener
			 {
				@Override
				public void mouseClicked(MouseEvent e) 
				{
					int x = e.getX();
					int y = e.getY();
					
					int mapColsIdx = ((y-BOARD_Y)+BOARD_CELL/2)/BOARD_CELL;
					int mapRowsIdx = ((x-BOARD_X)+BOARD_CELL/2)/BOARD_CELL;
					
					if(mapColsIdx>=0 && mapColsIdx<OmokMap.COLS)
						if(mapRowsIdx>=0 && mapColsIdx<OmokMap.ROWS)
						{
							if(isMyTurn == true && omokMap.getMap(mapColsIdx, mapRowsIdx) == EMPTY)
							{
								
								if(omokMap.setMap(mapColsIdx, mapRowsIdx, myStone) == true)
								{
									try 
									{
										dos.writeUTF("NEXT");
	
										dos.writeInt(mapColsIdx);
										dos.writeInt(mapRowsIdx);
										dos.writeInt(myStone);
										
										dos.flush();
									} 
									catch (IOException e1)
									{
										// TODO Auto-generated catch block
										e1.printStackTrace();
									}
			
									omokGui.updateBoard();
									
									isMyTurn = false;
								}
							}
						}
				}

				@Override
				public void mouseEntered(MouseEvent e) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void mouseExited(MouseEvent e) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void mousePressed(MouseEvent e) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					// TODO Auto-generated method stub
					
				}
			 }
		}
	}
	
	

	public static void main(String[] args)
	{
		try
		{
			omokGui = new OmokGui("Omok");
			omokGui.setVisible(true);
			omokGui.setResizable(false);
			omokGui.setVisible(true);
			omokGui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // x 버튼을 눌렀을때 종료
			
			String host = args[0];
			int port = Integer.parseInt(args[1]);

			OmokPlayer omokPlayer = new OmokPlayer(host,port);
			omokPlayer.setDaemon(true);
			omokPlayer.start();
			
			
		}
		catch(NumberFormatException e)
		{
			omokGui.setTurn("[ERROR]: 입력된 주소와 포트의 형식이 올바르지 않습니다. Ex: 127.0.0.1 7000");
			
			System.out.println("[ERROR]: 입력된 주소와 포트의 형식이 올바르지 않습니다. Ex: 127.0.0.1 7000");
		}
		catch(ArrayIndexOutOfBoundsException e)
		{
			omokGui.setTurn("[ERROR]: 주소와 포트를 입력해주세요.  Ex: 127.0.0.1 7000");
			
			System.out.println("[ERROR]: 주소와 포트를 입력해주세요.  Ex: 127.0.0.1 7000");
		}
	}
	
	@Override
	public void run()
	{
        String strCase = new String();
        boolean isOut = false;
        
		while(!isOut)
		{
			try
			{
				strCase = dis.readUTF();

				switch(strCase)
				{
				case "SET_ORDER":
					myStone = dis.readInt();
					
					if(myStone == WHITE)
					{
						myStoneStr = "백돌";
						enemyStone = BLACK;
					}
					else
					{
						myStoneStr = "흑돌";
						enemyStone = WHITE;
					}

					break;
							
				case "YOUR_TURN":
					isMyTurn = true;
					break;
					
				case "UPDATE_STONE":
					
					int col = dis.readInt();
					int row = dis.readInt();
					int stone = dis.readInt();
					
					omokMap.setMap(col, row, stone);
					omokGui.updateBoard();
				break;
				
				case "MATCH_FINISH":
					String strWinner = null;
					while(strWinner == null)
					{
						strWinner = dis.readUTF();
					}
					
					JOptionPane.showMessageDialog( omokGui, strWinner);
					omokGui.setTurn("게임이 끝났습니다.");
	
					isOut = true;
				break;
				
				default:
					if(strCase != null)
					{
						omokGui.setTurn(strCase +" -- 당신의 돌은 " + myStoneStr + " 입니다.");
					}
					break;
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
				break;
			}
		}

		try 
		{
			socket.close();
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
