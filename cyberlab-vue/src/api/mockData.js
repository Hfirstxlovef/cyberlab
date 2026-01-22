// 模拟数据
export const mockAchievements = {
  content: [
    {
      id: 1,
      type: 'red_team',
      teamName: '红队Alpha',
      drillName: '春季攻防演练',
      status: 'pending',
      submitTime: '2024-01-15T10:30:00',
      targetName: '192.168.1.100 Web服务器',
      targetInfo: '192.168.1.100 Web服务器',
      attackPath: '通过SQL注入获取管理员权限',
      attackMethod: 'SQL注入',
      vulnerabilities: 'SQL注入漏洞',
      cvssScore: 'high',
      description: '发现目标系统存在SQL注入漏洞，通过构造恶意SQL语句成功获取管理员权限。',
      screenshots: '/uploads/screenshot1.png,/uploads/screenshot2.png',
      proofFiles: '/uploads/poc.mp4,/uploads/logs.txt'
    },
    {
      id: 2,
      type: 'blue_team', 
      teamName: '蓝队Beta',
      drillName: '春季攻防演练',
      status: 'approved',
      submitTime: '2024-01-15T11:45:00',
      targetName: '防护系统',
      defenseType: '入侵检测',
      detectionMethod: 'IDS告警分析',
      responseProcess: '立即隔离受影响主机',
      description: '通过IDS系统检测到异常流量，及时发现并阻止了攻击行为。',
      screenshots: '/uploads/defense1.png',
      proofFiles: '/uploads/incident_report.pdf'
    },
    {
      id: 3,
      type: 'red_team',
      teamName: '红队Gamma',
      drillName: '夏季红蓝对抗',
      status: 'rejected',
      submitTime: '2024-01-16T14:20:00',
      targetName: '192.168.1.200 文件服务器',
      targetInfo: '192.168.1.200 文件服务器',
      attackPath: '权限提升攻击',
      attackMethod: '本地权限提升',
      vulnerabilities: '本地权限提升漏洞',
      cvssScore: 'medium',
      rejectReason: '证明材料不充分，需要提供更详细的攻击过程说明',
      description: '通过本地权限提升漏洞获取系统管理员权限。',
      screenshots: '/uploads/screenshot3.png',
      proofFiles: '/uploads/exploit.py'
    },
    {
      id: 4,
      type: 'blue_team',
      teamName: '蓝队Delta',
      drillName: '秋季实战演习',
      status: 'pending',
      submitTime: '2024-01-17T09:15:00',
      targetName: '网络边界',
      defenseType: '边界防护',
      detectionMethod: '防火墙日志分析',
      responseProcess: '更新防火墙规则，阻断恶意IP',
      description: '通过防火墙日志分析发现异常连接，及时更新安全策略。',
      screenshots: '/uploads/firewall1.png',
      proofFiles: '/uploads/firewall_logs.txt'
    }
  ],
  totalElements: 4,
  totalPages: 1,
  size: 10,
  number: 0
}

export const mockDrills = [
  {
    id: 1,
    name: '春季攻防演练',
    status: 'active',
    startTime: '2024-01-15T08:00:00',
    endTime: '2024-01-15T18:00:00',
    description: '春季网络安全攻防演练活动'
  },
  {
    id: 2,
    name: '夏季红蓝对抗',
    status: 'completed',
    startTime: '2024-01-16T08:00:00',
    endTime: '2024-01-16T18:00:00',
    description: '夏季红蓝对抗演练'
  }
]

export const mockStatistics = {
  totalSubmissions: 4,
  pendingCount: 2,
  approvedCount: 1,
  rejectedCount: 1,
  approvalRate: 25,
  redTeamSubmissions: 2,
  blueTeamSubmissions: 2,
  monthlyTrend: [
    { month: '1月', red: 5, blue: 3 },
    { month: '2月', red: 8, blue: 6 },
    { month: '3月', red: 12, blue: 9 },
    { month: '4月', red: 15, blue: 11 },
    { month: '5月', red: 18, blue: 14 },
    { month: '6月', red: 22, blue: 17 }
  ],
  typeDistribution: [
    { name: '漏洞发现', value: 35 },
    { name: '权限提升', value: 25 },
    { name: '横向移动', value: 20 },
    { name: '数据窃取', value: 15 },
    { name: '其他', value: 5 }
  ]
}

export const getMockStatistics = () => mockStatistics