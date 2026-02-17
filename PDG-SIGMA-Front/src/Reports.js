import './Reports.css';
import React, { useEffect, useState, useMemo, useCallback } from 'react';
import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';
import VerticalNavbar from './VerticalNavbar';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend,
  PieChart, Pie, Cell,
  LineChart, Line
} from 'recharts';
import {PopUp} from "./PopUp";
import { BACKEND_URL } from './config/ApiBackend';

function Reports() {

  const [monitorPerformanceDataOriginal, setMonitorPerformanceDataOriginal] = useState([]);
  const [professorDataOriginal, setProfessorDataOriginal] = useState([]);

  // console.log("Reports se está renderizando");
  const [asistenciaDataOriginal, setAsistenciaDataOriginal] = useState([]);

  // const [categoryUsageDataOriginal, setCategoryUsageDataOriginal] = useState([]);
  const [categoryReportData, setCategoryReportData] = useState([]);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [lastUpdatedAt, setLastUpdatedAt] = useState(null);

  //Pop up
  const [isOpen, setIsOpen] = useState(false)
  const [message, setMessage] = useState("")
  const [change, setChange] = useState(false)

  const [semester, setSemester] = useState('');
  const [program, setProgram] = useState('');
  const [course, setCourse] = useState('');
  const [professor, setProfessor] = useState('');
  const [monitor, setMonitor] = useState('');

  //Porcentaje efectividad por materia de monitores
  const[completedPercent, setCompletedPercent] = useState("")
  const[pendingPercent, setPendingPercent] = useState("")
  const[latePercent, setLatePercent] = useState("")
  const[porcentages, setPorcentages] = useState([{ completed: "0%", late: "0%", pending: "0%" }]);

  //Porcentaje efectividad por materia de profesores
  const[completedPercentProfessor, setCompletedPercentProfessor] = useState("")
  const[pendingPercentProfessor, setPendingPercentProfessor] = useState("")
  const[latePercentProfessor, setLatePercentProfessor] = useState("")
  const[porcentagesProfessor, setPorcentagesProfessor] = useState([{ completed: "0%", late: "0%", pending: "0%" }]);

  const fetchReportsData = useCallback(async () => {
    const user = localStorage.getItem('userId');
    const currentRole = localStorage.getItem('role');

    if (!user || !currentRole) {
      return;
    }

    setIsRefreshing(true);
    try {
      const monitorResponse = await fetch(`${BACKEND_URL}/monitoring/getMonitorsReport/${user}/${currentRole}`, {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': localStorage.getItem('token')
          },
      });
      const monitorJson = await monitorResponse.json();
      setMonitorPerformanceDataOriginal(Array.isArray(monitorJson) ? monitorJson : []);

      let professorList = [];
      const professorIds = [...new Set((Array.isArray(monitorJson) ? monitorJson : []).map(report => report.idProfessor).filter(Boolean))];
      for (const value of professorIds) {
        const professorResponse = await fetch(`${BACKEND_URL}/monitoring/getProfessorReport/${value}`,{
            method: 'GET',
            headers: { 'Content-Type': 'application/json' ,
                'Authorization':localStorage.getItem('token')
            },
        });
        const professorJson = await professorResponse.json();
        professorList = professorList.concat(Array.isArray(professorJson) ? professorJson : []);
      }
      setProfessorDataOriginal(professorList);

      const attendanceResponse = await fetch(`${BACKEND_URL}/monitoring/getAttendanceReport/${currentRole}/${user}`,{
          method: 'GET',
          headers: { 'Content-Type': 'application/json' ,
              'Authorization':localStorage.getItem('token')
          },
          });
      const attendanceJson = await attendanceResponse.json();
      setAsistenciaDataOriginal(Array.isArray(attendanceJson) ? attendanceJson : []);

      const url = `${BACKEND_URL}/monitoring/getCategoriesReport/${currentRole}/${user}`;
      const categoriesResponse = await fetch(url,{
          method: 'GET',
          headers: { 'Content-Type': 'application/json' ,
              'Authorization':localStorage.getItem('token')
          },
          });
      if (!categoriesResponse.ok) {
          const errorData = await categoriesResponse.json().catch(() => ({}));
          throw new Error(errorData.error || `Error ${categoriesResponse.status}`);
      }
      const categoriesJson = await categoriesResponse.json();
      setCategoryReportData(categoriesJson || null);
      setLastUpdatedAt(new Date());
    } catch (error) {
      console.error("Error actualizando reportes en tiempo real:", error);
      setCategoryReportData(null);
    } finally {
      setIsRefreshing(false);
    }
  }, []);

  useEffect(() => {
    setMessage("Usa los filtros de período, departamento, profesor o monitor para consultar el dashboard de reportes.")
    setIsOpen(true)
    setChange(true)
  }, [fetchReportsData]);

  const getValues = (data) =>{
    
    const list = [];
    monitorPerformanceDataOriginal.forEach((a) =>{

        if(data === "semester"){
            if(!list.includes(a.semester)){
                list.push(a.semester);
            }
        }else if(data === "courses" && program !== ''){
            if(!list.includes(a.course) && a.program === program){
                list.push(a.course);
            }
        }else if(data === "professors"){
          if(!list.includes(a.professor)
            && (!semester || a.semester === semester)
            && (!program || a.program === program)
            && (!course || a.course === course)
          ){
                list.push(a.professor);

            }
        }else if(data === "programs"){
            if(!list.includes(a.program)){
                list.push(a.program);
            }
        }else {
          if(!list.includes(a.name)
            && (!semester || a.semester === semester)
            && (!program || a.program === program)
            && (!course || a.course === course)
            && (!professor || a.professor === professor)
          ){
                list.push(a.name);
            }
        }
       
    });
    return list;
  }

  // const monitorPerformanceDataOriginal = [
  //   { name: 'Monitor A', Completadas: 12, Tardias: 3, Pendientes: 2, semestre: '2024-1', programa: 'Ingenieria de Sistemas', curso: 'POO', profesor: 'Claudia' },
  // ];

  const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042'];

  //  Función que aplica los filtros actuales
  const applyFilters = (data) => {
    if (!Array.isArray(data)) {
      console.warn("applyFilters recibió datos que no son un array:", data);
      return [];
    }

    return data.filter(d =>
      (!semester || d.semester === semester) &&
      (!program || d.program === program) &&
      (!course || d.course === course) &&
      (!professor || d.professor === professor) &&
      (!monitor || d.name === monitor)
    );
    
  };

  const applyAttendanceFilters = (data) => {
    if (!Array.isArray(data)) {
      console.warn("applyAttendanceFilters recibió datos que no son un array:", data);
      return [];
    }
    return data.filter(d => {
      if (!d) return false;

      const semesterMatch = !semester || d.semestre === semester;

      const courseMatch = !course ||
        (Array.isArray(d.asistencia_por_curso) && 
         d.asistencia_por_curso.some(item => item.curso === course));

      return semesterMatch && courseMatch;
    });
  };

  const filteredAttendanceData = useMemo(
    () => applyAttendanceFilters(asistenciaDataOriginal),
    [asistenciaDataOriginal, semester, course]
  );

  const dashboardMonitorData = useMemo(
    () => applyFilters(monitorPerformanceDataOriginal),
    [monitorPerformanceDataOriginal, semester, program, course, professor, monitor]
  );

  const dashboardProfessorData = useMemo(
    () => professorDataOriginal.filter(d =>
      (!semester || d.semester === semester) &&
      (!program || d.program === program) &&
      (!course || d.course === course) &&
      (!professor || d.name === professor)
    ),
    [professorDataOriginal, semester, program, course, professor]
  );

  const dashboardMetrics = useMemo(() => {
    const totalMonitors = new Set(dashboardMonitorData.map(d => d.name)).size;
    const totalProfessors = new Set(dashboardMonitorData.map(d => d.professor)).size;

    const monitorTotals = dashboardMonitorData.reduce((acc, item) => {
      acc.completed += item.completed || 0;
      acc.pending += item.pending || 0;
      acc.late += item.late || 0;
      return acc;
    }, { completed: 0, pending: 0, late: 0 });

    const professorTotals = dashboardProfessorData.reduce((acc, item) => {
      acc.completed += item.completed || 0;
      acc.pending += item.pending || 0;
      acc.late += item.late || 0;
      return acc;
    }, { completed: 0, pending: 0, late: 0 });

    const monitorBase = monitorTotals.completed + monitorTotals.pending + monitorTotals.late;
    const professorBase = professorTotals.completed + professorTotals.pending + professorTotals.late;

    return {
      totalMonitors,
      totalProfessors,
      monitorCompliance: monitorBase > 0 ? Math.round((monitorTotals.completed / monitorBase) * 100) : 0,
      professorCompliance: professorBase > 0 ? Math.round((professorTotals.completed / professorBase) * 100) : 0
    };
  }, [dashboardMonitorData, dashboardProfessorData]);

  const chartReadyAttendanceData = filteredAttendanceData.map(d => {
    let displayValue;
    let attendance;
    if (course) {
      const courseEntry = d.asistencia_por_curso?.find(item => item.curso === course);
      displayValue = courseEntry ? courseEntry.cantidad : 0;
      attendance = courseEntry? courseEntry.estudiantes : [];
    } else {
      displayValue = d.total_mes;
      attendance = [];
    }

    return {
      mes: d.mes,
      semestre: d.semestre, 
      asistencia: displayValue,
      asistentes:attendance
    };
  });

  const lineName = course ? `Asistentes - ${course}` : "Total Asistentes";

  const pieChartData = useMemo(() => {
    if (!categoryReportData) {
      return [];
    }
    if(program ==='' || semester === '') {
      return [];
    }

    if (course) {
      const courseDetail = categoryReportData.detalle_por_curso?.find(
        (detail) => detail.curso === course
      );
    
      if (courseDetail && Array.isArray(courseDetail.categorias)) {
        return courseDetail.categorias.map(cat => ({
          categoria: cat.categoria,
          cantidad_total: cat.cantidad 
        }))
        .slice(0, 5);
      } else {
        return [];
      }
    } else {
      const totals = categoryReportData?.totales_por_categoria;
      if (Array.isArray(totals)) {
        console.log("Calculando pieChartData: Devolviendo totales:", totals);
        return totals.slice(0, 5);
      } else {
        console.warn("totales_por_categoria no es un arreglo válido:", totals);
        return [];
      }
    }
    
  }, [categoryReportData, course, program, semester]); 

    const categoryChartTitle = course ? `Uso de Categorías - ${course}` : "Top 5 de actividades por categoría";

   const exportToCSV = (data, filename, filters) => {
    if (!data || data.length === 0) return;

    const csvRows = [];

    // Tabla principal
    const headers = Object.keys(data[0]);
    csvRows.push(headers.join(','));

    data.forEach(row => {
      const values = headers.map(header => `"${row[header] ?? ''}"`);
      csvRows.push(values.join(','));
    });

    // Separación visual
    csvRows.push('', '', 'Filtros aplicados:');

    // Filtros con etiquetas
    csvRows.push('Filtro,Valor');
    csvRows.push(`Semestre,${filters.semester}`);
    csvRows.push(`Programa,${filters.program}`);
    csvRows.push(`Curso,${filters.course}`);

    if (filters.professor && filters.professor.trim() !== '') {
      csvRows.push(`Profesor,${filters.professor}`);
    }

    if (filters.monitor && filters.monitor.trim() !== '') {
      csvRows.push(`Monitor,${filters.monitor}`);
    }

    // Crear blob y descargar
    const csvString = csvRows.join('\n');
    const blob = new Blob([csvString], { type: 'text/csv;charset=utf-8;' });
    const url = window.URL.createObjectURL(blob);

    const link = document.createElement('a');
    link.href = url;
    link.download = `${filename}.csv`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const exportToPDF = (data, filename, filters) => {
    if (!data || data.length === 0) return;

    const normalizeValue = (value) => {
      if (value === null || value === undefined) return '';
      if (Array.isArray(value)) return value.join(' | ');
      if (typeof value === 'object') return JSON.stringify(value);
      return String(value);
    };

    const pdf = new jsPDF({ orientation: 'landscape', unit: 'pt', format: 'a4' });
    const headers = Object.keys(data[0]);
    const rows = data.map((row) => headers.map((header) => normalizeValue(row[header])));

    pdf.setFontSize(14);
    pdf.text(`Reporte: ${filename.replaceAll('_', ' ')}`, 40, 40);
    pdf.setFontSize(10);
    pdf.text(
      `Generado: ${new Date().toLocaleString('es-CO')} | Periodo: ${filters.semester || 'Todos'} | Departamento: ${filters.program || 'Todos'} | Profesor: ${filters.professor || 'Todos'} | Monitor: ${filters.monitor || 'Todos'}`,
      40,
      58
    );

    autoTable(pdf, {
      startY: 72,
      head: [headers],
      body: rows,
      styles: { fontSize: 8, cellPadding: 4 },
      headStyles: { fillColor: [84, 84, 232] }
    });

    pdf.save(`${filename}.pdf`);
  };
  //const categoryUsageData = applyFilters(categoryUsageDataOriginal);
  const asistenciaData = chartReadyAttendanceData;

  
  const semestersToShow = getValues("semester");
  const professorsToShow = getValues("professors");
  const programsToShow = getValues("programs");
  const monitorsToShow = getValues("monitors");


  const monitorPerformanceData = dashboardMonitorData;
  const professorData = dashboardProfessorData;

  //Porcentaje actividades monitores


  useEffect(() => {
    if (monitorPerformanceData.length > 0) {
  
      // Calcula los porcentajes globales para monitores
      let totalCompleted = 0;
      let totalLate = 0;
      let totalPending = 0;
  
      monitorPerformanceData.forEach(item => {
        totalCompleted += item.completed || 0;
        totalLate += item.late || 0;
        totalPending += item.pending || 0;
      });
  
      const total = totalCompleted + totalLate + totalPending;
  
      if (total > 0) {
        setCompletedPercent(`${Math.round((totalCompleted / total) * 100)}%`);
        setPendingPercent(`${Math.round((totalPending / total) * 100)}%`);
        setLatePercent(`${Math.round((totalLate / total) * 100)}%`);
        setPorcentages([{
          completed: `${Math.round((totalCompleted / total) * 100)}%`,
          pending: `${Math.round((totalPending / total) * 100)}%`,
          late: `${Math.round((totalLate / total) * 100)}%`
        }]);
      }
    }
  }, [monitorPerformanceData]);
  
  //Porcentaje actividades profesores

useEffect(() => {
  if (professorData.length > 0) {
    let completed = 0;
    let late = 0;
    let pending = 0;
    
    professorData.forEach(item => {
      completed += item.completed || 0;
      late += item.late || 0;
      pending += item.pending || 0;
    });

    const total = completed + late + pending;

    if (total > 0) {
      const completedPercent = Math.round((completed / total) * 100);
      const latePercent = Math.round((late / total) * 100);
      const pendingPercent = Math.round((pending / total) * 100);

      setPorcentagesProfessor([{
        completed: `${completedPercent}%`,
        late: `${latePercent}%`,
        pending: `${pendingPercent}%`
      }]);

      setCompletedPercentProfessor(`${completedPercent}%`);
      setPendingPercentProfessor(`${pendingPercent}%`);
      setLatePercentProfessor(`${latePercent}%`);
    }
  }
}, [professorData]);


  const handleClose = () =>{
      setIsOpen(!isOpen)
      setChange(!change)
  }


  return (
    <div className="main">
      <PopUp
        show={isOpen}
        onClose={() => handleClose()}
      >{message}
      </PopUp>
      <div className="reports-top-bar">
        <h2 className="reports-title">Dashboard de Reportes de Cumplimiento</h2>
        <div className="realtime-status">
          <span>{isRefreshing ? 'Actualizando datos...' : `Última actualización: ${lastUpdatedAt ? lastUpdatedAt.toLocaleTimeString('es-CO') : 'pendiente'}`}</span>
          <button
            type="button"
            className="chart-download-button"
            onClick={fetchReportsData}
            disabled={isRefreshing}
          >
            {isRefreshing ? 'Actualizando...' : 'Actualizar ahora'}
          </button>
        </div>
        <div className="filters-container">
          <div className="filter-group">
            <select value={semester} onChange={(e) => setSemester(e.target.value)}>
                  <option value="">Período</option>
                  {semestersToShow.map((semester, index) => (
                      <option key={index} value={semester}>
                      {semester}
                      </option>
                  ))}
            </select>
          </div>
          <div className="filter-group">
            <select value={program} onChange={(e) => setProgram(e.target.value)}>
              <option value="">Departamento</option>
                {programsToShow.map((program, index) => (
                    <option key={index} value={program}>
                    {program}
                    </option>
                ))}
            </select>
          </div>
          <div className="filter-group">
            <select value={professor} onChange={(e) => setProfessor(e.target.value)}>
            <option value="">Profesor</option>
                {professorsToShow.map((professor, index) => (
                    <option key={index} value={professor}>
                    {professor}
                    </option>
                ))}
            </select>
          </div>
          <div className="filter-group">
            <select value={monitor} onChange={(e) => setMonitor(e.target.value)}>
              <option value="">Monitor</option>
              {monitorsToShow.map((monitor, index) => (
                    <option key={index} value={monitor}>
                    {monitor}
                    </option>
                ))}
            </select>
          </div>
          <div className="filter-group">
            <button
              type="button"
              className="chart-download-button"
              onClick={() => {
                setSemester('');
                setProgram('');
                setCourse('');
                setProfessor('');
                setMonitor('');
              }}
            >
              Limpiar filtros
            </button>
          </div>
        </div>
      </div>
      <div className="reports-container">
        <VerticalNavbar />
        <div className="reports-content">
          <div className="dashboard-summary-card">
            <h3>Resumen general</h3>
            <div className="dashboard-summary-grid">
              <div className="dashboard-metric-card">
                <span>Profesores en reporte</span>
                <strong>{dashboardMetrics.totalProfessors}</strong>
              </div>
              <div className="dashboard-metric-card">
                <span>Monitores en reporte</span>
                <strong>{dashboardMetrics.totalMonitors}</strong>
              </div>
              <div className="dashboard-metric-card">
                <span>Cumplimiento de profesores</span>
                <strong>{dashboardMetrics.professorCompliance}%</strong>
              </div>
              <div className="dashboard-metric-card">
                <span>Cumplimiento de monitores</span>
                <strong>{dashboardMetrics.monitorCompliance}%</strong>
              </div>
            </div>
          </div>
          
    {/* Gráfico de barras - MONITOR */}
    <div className="chart-card">
      <h3>Rendimiento de monitores</h3>
      <BarChart width={500} height={300} data={monitorPerformanceData}>
        <CartesianGrid strokeDasharray="3 3" />
        
        {/* Mostrar solo el nombre del monitor */}
        <XAxis 
          dataKey="nameAndCourse" 
          tickFormatter={(value) => value.split(' ')[0]} 
        />
        
        <YAxis />
        
        {/* Tooltip */}
        <Tooltip 
          formatter={(value, name, props) => {
            const map = {
              completed: 'Completado',
              pending: 'Pendiente',
              late: 'Tarde',
            };

            const payload = props.payload;

            if (payload && typeof value === 'number') {
              const completed = payload.completed || 0;
              const pending = payload.pending || 0;
              const late = payload.late || 0;
              const total = completed + pending + late;

              const percentage = total > 0 ? Math.round((value / total) * 100) : 0;

              return [`${value} actividades - ${percentage}%`, map[name] || name];
            }

            return [`${value}`, map[name] || name];
          }}
          labelFormatter={(label) => `${label}`}
        />

        {/* Legend */}
        <Legend 
          formatter={(value) => {
            const map = {
              completed: 'Completado',
              pending: 'Pendiente',
              late: 'Tarde',
            };
            return map[value] || value;
          }} 
        />
        
        {/* Barra de Completado */}
        <Bar dataKey="completed" stackId="a" fill="rgb(0, 196, 159)" />

        {/* Barra de Pendiente */}
        <Bar dataKey="pending" stackId="a" fill="rgb(255, 82, 82)"/>

        {/* Barra de Tarde */}
        <Bar dataKey="late" stackId="a" fill="rgb(255, 187, 40)" />
           
      </BarChart>

          <div className="chart-download-container">
            <button className="chart-download-button" onClick={() => exportToCSV(monitorPerformanceData, 'Rendimiento_Monitores', {
                  semester,
                  program,
                  course,
                  professor,
                  monitor
                })
              }>Descargar CSV</button>
            <button className="chart-download-button" onClick={() => exportToPDF(monitorPerformanceData, 'Rendimiento_Monitores', {
                  semester,
                  program,
                  course,
                  professor,
                  monitor
                })
              }>Descargar PDF</button>
          </div>
        </div>


         {/* Porcentaje de tareas - MONITOR*/}
         <div className="chart-card">
            <h3>Tareas completadas, tardías y pendientes de monitores</h3>
            <div className="reports-summary">
              <div className="summary-card completadas">
                <h4>Completadas</h4>
                <p>{completedPercent}</p>
              </div>
              <div className="summary-card tardias">
                <h4>Completadas tardías</h4>
                <p>{latePercent}</p>
              </div>
              <div className="summary-card pendientes">
                <h4>Pendientes</h4>
                <p>{pendingPercent}</p>
              </div>
            </div>
            <div className="chart-download-container">
              <button className="chart-download-button" onClick={() => exportToCSV(porcentages, 'Resumen_Tareas_Monitor', {
                  semester,
                  program,
                  course,
                  professor,
                  monitor
                })
              }>Descargar CSV</button>
              <button className="chart-download-button" onClick={() => exportToPDF(porcentages, 'Resumen_Tareas_Monitor', {
                  semester,
                  program,
                  course,
                  professor,
                  monitor
                })
              }>Descargar PDF</button>
            </div>
          </div>

          {/* Gráfico de pastel*/}
          <div className="chart-card">
            <h3>{categoryChartTitle}</h3>
             <PieChart width={400} height={300}>
              <Pie
                data={pieChartData}
                cx="50%"
                cy="50%"
                outerRadius={100}
                dataKey="cantidad_total"
                nameKey="categoria"
                label={({ categoria }) => categoria}
              >
                {pieChartData.map((entry, index) => (
                  <Cell key={`cell-${index}-${entry.categoria}`} fill={COLORS[index % COLORS.length]} />
                ))}
              </Pie>
              <Tooltip 
                formatter={(value, name, props) => {
                  const total = pieChartData.reduce((acc, curr) => acc + curr.cantidad_total, 0);
                  const percent = ((value / total) * 100).toFixed(1);
                  return [`${value} actividades (${percent}%)`, 'Cantidad'];
                }} 
              />
            </PieChart>
            <div className="chart-download-container">
              <button className="chart-download-button" onClick={() => exportToCSV(pieChartData, 'Categorias_Por_Curso', {
                  semester,
                  program,
                  course,
                  professor,
                  monitor
                })
              }>Descargar CSV</button>
              <button className="chart-download-button" onClick={() => exportToPDF(pieChartData, 'Categorias_Por_Curso', {
                  semester,
                  program,
                  course,
                  professor,
                  monitor
                })
              }>Descargar PDF</button>
            </div>
          </div>

          {/* Asistencias */}
          <div className="chart-card">
            <h3> {`Asistencia a monitorías ${course ? `(${course})` : '(mensual)'}`}</h3>
            <LineChart width={500} height={300} data={asistenciaData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="mes" />
              <YAxis allowDecimals={false} />
              <Tooltip formatter={(value, name, props) => [`${value} asistentes`, lineName]} />
              <Legend />
              <Line
                type="monotone"
                dataKey="asistencia"
                stroke="#8884d8"
                name={lineName}
                activeDot={{ r: 8 }}
              />
            </LineChart>
            <div className="chart-download-container">
              <button className="chart-download-button" onClick={() => exportToCSV(asistenciaData, `Asistencia_${lineName.replace(' ', '_')}`, {
                  semester,
                  program,
                  course,
                  professor,
                  monitor
                })
              }>Descargar CSV</button>
              <button className="chart-download-button" onClick={() => exportToPDF(asistenciaData, `Asistencia_${lineName.replace(' ', '_')}`, {
                  semester,
                  program,
                  course,
                  professor,
                  monitor
                })
              }>Descargar PDF</button>
              {/* datos filtrados originales*/}
              {/* <button className="chart-download-button" onClick={() => exportToCSV(filteredAttendanceData, 'Asistencia_Detallada_Filtrada')}>Descargar Detalle Filtrado</button> */}
            </div>
          </div>

        {/* Gráfico de barras - PROFESOR */}
        <div className="chart-card">
          <h3>Rendimiento de profesores</h3>
            <BarChart width={500} height={300} data={professorData}>
              <CartesianGrid strokeDasharray="3 3" />
              
              {/* Mostrar solo el nombre (jeje sin el apellido) en el eje X */}
              <XAxis 
                dataKey="name" 
                tickFormatter={(value) => value.split(' ')[0]} 
              />
              
              <YAxis />

              <Tooltip 
                formatter={(value, name, props) => {
                  const map = {
                    completed: 'Completado',
                    pending: 'Pendiente',
                    late: 'Tarde',
                  };

                  const payload = props.payload;

                  if (payload && typeof value === 'number') {
                    const completed = payload.completed || 0;
                    const pending = payload.pending || 0;
                    const late = payload.late || 0;
                    const total = completed + pending + late;

                    const percentage = total > 0 ? Math.round((value / total) * 100) : 0;

                    return [`${value} actividades - ${percentage}%`, map[name] || name];
                  }

                  return [`${value}`, map[name] || name];
                }}

                labelFormatter={(label) => {
                  const parts = label.split(' ');
                  if (parts.length >= 3) {
                    const nombreCompleto = `${parts[0]} ${parts[1]}`;
                    const nombreCurso = parts.slice(2).join(' ');
                    return `${nombreCompleto} ${nombreCurso}`;
                  }
                  return label; // fallback
                }}
              />



              <Legend 
                formatter={(value) => {
                  const map = {
                    completed: 'Completado',
                    pending: 'Pendiente',
                    late: 'Tarde',
                  };
                  return map[value] || value;
                }} 
              />

              <Bar dataKey="completed" stackId="a" fill="rgb(0, 196, 159)" />
              <Bar dataKey="pending" stackId="a" fill="rgb(255, 82, 82)" />
              <Bar dataKey="late" stackId="a" fill="rgb(255, 187, 40)" />
            </BarChart>

          <div className="chart-download-container">
            <button className="chart-download-button" onClick={() => exportToCSV(professorData, 'Rendimiento_Profesores', {
                  semester,
                  program,
                  course,
                  professor,
                  monitor
                })
              }>Descargar CSV</button>
            <button className="chart-download-button" onClick={() => exportToPDF(professorData, 'Rendimiento_Profesores', {
                  semester,
                  program,
                  course,
                  professor,
                  monitor
                })
              }>Descargar PDF</button>
          </div>
        </div>


         {/* Porcentaje de tareas - PROFESOR */}
         <div className="chart-card">
            <h3>Tareas completadas, tardías y pendientes de profesores</h3>
            <div className="reports-summary">
              <div className="summary-card completadas">
                <h4>Completadas</h4>
                <p>{completedPercentProfessor}</p>
              </div>
              <div className="summary-card tardias">
                <h4>Completadas tardías</h4>
                <p>{latePercentProfessor}</p>
              </div>
              <div className="summary-card pendientes">
                <h4>Pendientes</h4>
                <p>{pendingPercentProfessor}</p>
              </div>
            </div>
            <div className="chart-download-container">
              <button className="chart-download-button" onClick={() => exportToCSV(porcentagesProfessor, 'Resumen_Tareas_Profesor', {
                  semester,
                  program,
                  course,
                  professor,
                  monitor
                })
              }>Descargar CSV</button>
              <button className="chart-download-button" onClick={() => exportToPDF(porcentagesProfessor, 'Resumen_Tareas_Profesor', {
                  semester,
                  program,
                  course,
                  professor,
                  monitor
                })
              }>Descargar PDF</button>
            </div>
          </div>

        </div>
      </div>
    </div>
  );
}

export default Reports;